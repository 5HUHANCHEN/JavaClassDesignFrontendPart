package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.EUserType;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.models.User;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.repositorys.UserTypeRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(
            TeacherRepository teacherRepository,
            PersonRepository personRepository,
            UserRepository userRepository,
            UserTypeRepository userTypeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.teacherRepository = teacherRepository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public DataResponse getTeacherList(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        if (numName == null) {
            numName = "";
        }

        List<Teacher> teachers = teacherRepository.findTeacherListByNumName(numName);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Teacher teacher : teachers) {
            dataList.add(getTeacherMap(teacher));
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse getTeacherInfo(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null || personId <= 0) {
            personId = CommonMethod.getPersonId();
        }
        if (personId == null) {
            return CommonMethod.getReturnMessageError("教师不存在！");
        }
        Optional<Teacher> op = teacherRepository.findById(personId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("教师不存在！");
        }
        return CommonMethod.getReturnData(getTeacherMap(op.get()));
    }

    private Map<String, Object> getTeacherMap(Teacher teacher) {
        Map<String, Object> m = new HashMap<>();
        if (teacher == null) {
            return m;
        }
        Person p = teacher.getPerson();
        m.put("personId", teacher.getPersonId());
        m.put("num", p == null ? "" : p.getNum());
        m.put("name", p == null ? "" : p.getName());
        m.put("dept", p == null ? "" : p.getDept());
        m.put("card", p == null ? "" : p.getCard());
        m.put("gender", p == null ? "" : p.getGender());
        m.put("birthday", p == null ? "" : p.getBirthday());
        m.put("email", p == null ? "" : p.getEmail());
        m.put("phone", p == null ? "" : p.getPhone());
        m.put("address", p == null ? "" : p.getAddress());
        m.put("introduce", p == null ? "" : p.getIntroduce());
        m.put("title", teacher.getTitle());
        m.put("degree", teacher.getDegree());
        return m;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataResponse teacherSave(DataRequest dataRequest) {
        try {
            Integer personId = dataRequest.getInteger("personId");
            String num = safeTrim(dataRequest.getString("num"));
            String name = safeTrim(dataRequest.getString("name"));

            if (num == null || num.isEmpty()) {
                return CommonMethod.getReturnMessageError("工号不能为空！");
            }
            if (name == null || name.isEmpty()) {
                return CommonMethod.getReturnMessageError("姓名不能为空！");
            }

            String dept = dataRequest.getString("dept");
            String title = dataRequest.getString("title");
            String degree = dataRequest.getString("degree");
            String card = dataRequest.getString("card");
            String gender = dataRequest.getString("gender");
            String birthday = dataRequest.getString("birthday");
            String email = dataRequest.getString("email");
            String phone = dataRequest.getString("phone");
            String address = dataRequest.getString("address");
            String introduce = dataRequest.getString("introduce");

            Teacher teacher;
            Person person;

            if (personId != null) {
                Optional<Teacher> op = teacherRepository.findById(personId);
                if (op.isEmpty()) {
                    return CommonMethod.getReturnMessageError("教师不存在，无法修改！");
                }
                teacher = op.get();
                person = teacher.getPerson();
                if (person == null) {
                    return CommonMethod.getReturnMessageError("教师基础信息不存在！");
                }

                Optional<Person> numOp = personRepository.findByNum(num);
                if (numOp.isPresent() && !numOp.get().getPersonId().equals(personId)) {
                    return CommonMethod.getReturnMessageError("工号已存在，不能重复保存！");
                }
            } else {
                Optional<Person> numOp = personRepository.findByNum(num);
                if (numOp.isPresent()) {
                    return CommonMethod.getReturnMessageError("工号已存在，不能重复新增！");
                }

                person = new Person();
                person.setType("2");
                teacher = new Teacher();
                teacher.setPerson(person);
            }

            person.setNum(num);
            person.setName(name);
            person.setDept(dept);
            person.setCard(card);
            person.setGender(gender);
            person.setBirthday(birthday);
            person.setEmail(email);
            person.setPhone(phone);
            person.setAddress(address);
            person.setIntroduce(introduce);
            teacher.setTitle(title);
            teacher.setDegree(degree);

            teacherRepository.save(teacher);
            Integer savedPersonId = teacher.getPerson().getPersonId();
            syncTeacherUser(savedPersonId, teacher.getPerson(), num);

            return CommonMethod.getReturnData(savedPersonId, "保存成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonMethod.getReturnMessageError("保存失败！" + e.getMessage());
        }
    }

    private void syncTeacherUser(Integer personId, Person person, String userName) {
        Optional<User> userOp = userRepository.findByPersonPersonId(personId);
        User user = userOp.orElseGet(User::new);
        user.setPersonId(personId);
        user.setPerson(person);
        user.setUserName(userName);
        user.setUserType(userTypeRepository.findByName(EUserType.ROLE_TEACHER.name()));
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode("123456"));
        }
        if (user.getLoginCount() == null) {
            user.setLoginCount(0);
        }
        if (user.getCreatorId() == null) {
            user.setCreatorId(CommonMethod.getPersonId());
        }
        userRepository.save(user);
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
        userRepository.findByPersonPersonId(personId).ifPresent(userRepository::delete);
        teacherRepository.delete(teacher);
        if (person != null) {
            personRepository.delete(person);
        }

        return CommonMethod.getReturnMessageOK("删除成功！");
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}

