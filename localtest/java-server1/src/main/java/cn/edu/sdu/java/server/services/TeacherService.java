package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final PersonRepository personRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          PersonRepository personRepository) {
        this.teacherRepository = teacherRepository;
        this.personRepository = personRepository;
    }

    public DataResponse getTeacherList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        if (numName == null) {
            numName = "";
        }

        List<Teacher> tList = teacherRepository.findTeacherListByNumName(numName);
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Teacher t : tList) {
            Map<String, Object> m = new HashMap<>();
            Person p = t.getPerson();

            m.put("personId", t.getPersonId());
            m.put("num", p == null ? "" : p.getNum());
            m.put("name", p == null ? "" : p.getName());
            m.put("dept", p == null ? "" : p.getDept());
            m.put("title", t.getTitle());
            m.put("degree", t.getDegree());

            dataList.add(m);
        }

        return CommonMethod.getReturnData(dataList);
    }

    /**
     * 关键修改：
     * 1. 添加 @Transactional：确保数据库操作的原子性。
     * 2. 优化保存逻辑：利用 @MapsId + Cascade 特性，统一通过 save(teacher) 完成保存。
     */
    @Transactional(rollbackFor = Exception.class)
    public DataResponse teacherSave(DataRequest dataRequest) {
        try {
            Integer personId = dataRequest.getInteger("personId");
            String num = dataRequest.getString("num");
            String name = dataRequest.getString("name");
            String dept = dataRequest.getString("dept");
            String title = dataRequest.getString("title");
            String degree = dataRequest.getString("degree");

            // 数据清洗
            if (num != null) num = num.trim();
            if (name != null) name = name.trim();

            // 基础校验
            if (num == null || num.isEmpty()) {
                return CommonMethod.getReturnMessageError("工号不能为空！");
            }
            if (name == null || name.isEmpty()) {
                return CommonMethod.getReturnMessageError("姓名不能为空！");
            }

            Teacher teacher;
            Person person;

            // --- 场景 A: 修改现有教师 ---
            if (personId != null) {
                Optional<Teacher> op = teacherRepository.findById(personId);
                if (op.isEmpty()) {
                    return CommonMethod.getReturnMessageError("教师不存在，无法修改！");
                }
                teacher = op.get();
                person = teacher.getPerson();

                if (person == null) {
                    return CommonMethod.getReturnMessageError("数据异常：该教师缺少关联的人员信息！");
                }

                // 检查工号是否被其他人占用
                Optional<Person> opNum = personRepository.findByNum(num);
                if (opNum.isPresent() && !opNum.get().getPersonId().equals(personId)) {
                    return CommonMethod.getReturnMessageError("工号 [" + num + "] 已存在，不能重复保存！");
                }

                // 更新属性
                person.setNum(num);
                person.setName(name);
                person.setDept(dept);
                // type 通常不变，如果需要也可以更新
                // person.setType("2");

                teacher.setTitle(title);
                teacher.setDegree(degree);

                // 保存：级联更新 Person
                teacherRepository.save(teacher);
                System.out.println(">>> 修改成功: PersonID=" + personId);

            }
            // --- 场景 B: 新增教师 ---
            else {
                // 1. 先检查工号是否存在
                Optional<Person> opNum = personRepository.findByNum(num);
                if (opNum.isPresent()) {
                    return CommonMethod.getReturnMessageError("工号 [" + num + "] 已存在，不能重复新增！");
                }

                // 2. 创建 Person 对象 (此时尚未持久化，无 ID)
                person = new Person();
                person.setType("2"); // 假设 2 代表教师
                person.setNum(num);
                person.setName(name);
                person.setDept(dept);

                // 3. 创建 Teacher 对象
                teacher = new Teacher();
                teacher.setTitle(title);
                teacher.setDegree(degree);

                // 4. 【关键步骤】建立关联
                teacher.setPerson(person);
                // 如果是双向关联，且 Person 类中有 teacher 字段，建议也设置反向引用
                // person.setTeacher(teacher);

                // 5. 【关键步骤】只保存 Teacher
                // 由于 @MapsId 和 cascade=ALL 的存在：
                // Hibernate 会先执行 INSERT INTO person (...) 获取生成的 ID
                // 然后自动将该 ID 赋值给 teacher.personId
                // 最后执行 INSERT INTO teacher (person_id, title, degree) VALUES (...)
                teacherRepository.save(teacher);

                // 此时 person 对象中应该已经被 Hibernate 填充了 personId
                Integer generatedId = person.getPersonId();
                if (generatedId == null) {
                    // 极端情况下可能需要 refresh，但通常不需要
                    // personRepository.refresh(person);
                    generatedId = person.getPersonId();
                }

                System.out.println(">>> 新增成功: Generated PersonID=" + generatedId);
            }

            return CommonMethod.getReturnData(person.getPersonId(), "保存成功！");

        } catch (Exception e) {
            System.out.println("===== teacherSave 发生异常，事务将回滚 =====");
            e.printStackTrace();
            return CommonMethod.getReturnMessageError("保存失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DataResponse teacherDelete(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null) {
            return CommonMethod.getReturnMessageError("personId 不能为空！");
        }

        Optional<Teacher> op = teacherRepository.findById(personId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("教师不存在！");
        }

        Teacher teacher = op.get();
        Person person = teacher.getPerson();

        // 由于配置了 cascade = ALL (或者在删除逻辑中手动处理)
        // 删除 Teacher 时，如果配置了 orphanRemoval 或 cascade 包含 REMOVE，Person 也会被删除
        // 为了保险起见，这里显式删除
        teacherRepository.delete(teacher);

        if (person != null) {
            // 如果 cascade 没配删除，这里手动删
            // 注意：如果 teacher 删除成功但 person 外键约束还在，可能会报错
            // 通常 @MapsId 共享主键，删了 teacher，person 就可以删了
            personRepository.delete(person);
        }

        return CommonMethod.getReturnMessageOK("删除成功！");
    }
}