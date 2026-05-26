package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.EUserType;
import cn.edu.sdu.java.server.models.FamilyMember;
import cn.edu.sdu.java.server.models.Fee;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.User;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.FamilyMemberRepository;
import cn.edu.sdu.java.server.repositorys.FeeRepository;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.UserRepository;
import cn.edu.sdu.java.server.repositorys.UserTypeRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final PersonRepository personRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final PasswordEncoder encoder;
    private final FeeRepository feeRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SystemService systemService;
    private final ScoreRepository scoreRepository;

    public StudentService(PersonRepository personRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          UserTypeRepository userTypeRepository,
                          PasswordEncoder encoder,
                          FeeRepository feeRepository,
                          FamilyMemberRepository familyMemberRepository,
                          SystemService systemService,
                          ScoreRepository scoreRepository) {
        this.personRepository = personRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.encoder = encoder;
        this.feeRepository = feeRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.systemService = systemService;
        this.scoreRepository = scoreRepository;
    }

    public Map<String, Object> getMapFromStudent(Student student) {
        Map<String, Object> data = new HashMap<>();
        if (student == null) {
            return data;
        }
        data.put("major", student.getMajor());
        data.put("className", student.getClassName());
        Person person = student.getPerson();
        if (person == null) {
            return data;
        }
        data.put("personId", student.getPersonId());
        data.put("num", person.getNum());
        data.put("name", person.getName());
        data.put("dept", person.getDept());
        data.put("card", person.getCard());
        data.put("gender", person.getGender());
        data.put("genderName", ComDataUtil.getInstance().getDictionaryLabelByValue("XBM", person.getGender()));
        data.put("birthday", person.getBirthday());
        data.put("email", person.getEmail());
        data.put("phone", person.getPhone());
        data.put("address", person.getAddress());
        data.put("introduce", person.getIntroduce());
        return data;
    }

    public List<Map<String, Object>> getStudentMapList(String numName) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<Student> studentList = studentRepository.findStudentListByNumName(numName);
        if (studentList == null || studentList.isEmpty()) {
            return dataList;
        }
        for (Student student : studentList) {
            dataList.add(getMapFromStudent(student));
        }
        return dataList;
    }

    public DataResponse getStudentList(DataRequest dataRequest) {
        return CommonMethod.getReturnData(getStudentMapList(dataRequest.getString("numName")));
    }

    public DataResponse studentDelete(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        if (personId == null || personId <= 0) {
            return CommonMethod.getReturnMessageOK();
        }
        Optional<Student> studentOptional = studentRepository.findById(personId);
        if (studentOptional.isPresent()) {
            Student student = studentOptional.get();
            userRepository.findById(personId).ifPresent(userRepository::delete);
            Person person = student.getPerson();
            studentRepository.delete(student);
            if (person != null) {
                personRepository.delete(person);
            }
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse getStudentInfo(DataRequest dataRequest) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            Integer requestPersonId = dataRequest.getInteger("personId");
            if (requestPersonId != null && currentPersonId != null && !requestPersonId.equals(currentPersonId)) {
                return CommonMethod.getReturnMessageError("只能查看当前登录学生的信息！");
            }
            dataRequest.add("personId", currentPersonId);
        }
        Integer personId = dataRequest.getInteger("personId");
        Student student = null;
        if (personId != null) {
            student = studentRepository.findById(personId).orElse(null);
        }
        return CommonMethod.getReturnData(getMapFromStudent(student));
    }

    public DataResponse studentEditSave(DataRequest dataRequest) {
        if ("ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            Integer currentPersonId = CommonMethod.getPersonId();
            Integer requestPersonId = dataRequest.getInteger("personId");
            if (requestPersonId != null && currentPersonId != null && !requestPersonId.equals(currentPersonId)) {
                return CommonMethod.getReturnMessageError("只能修改当前登录学生的信息！");
            }
            dataRequest.add("personId", currentPersonId);
        }

        Integer personId = dataRequest.getInteger("personId");
        Map<String, Object> form = dataRequest.getMap("form");
        String num = CommonMethod.getString(form, "num");
        Student student = null;
        boolean isNew = false;
        if (personId != null) {
            student = studentRepository.findById(personId).orElse(null);
        }

        Optional<Person> personOptional = personRepository.findByNum(num);
        if (personOptional.isPresent() && (student == null || !num.equals(student.getPerson().getNum()))) {
            return CommonMethod.getReturnMessageError("学号已存在，不能重复创建学生账号！");
        }

        Person person;
        if (student == null) {
            person = new Person();
            person.setNum(num);
            person.setType("1");
            personRepository.saveAndFlush(person);

            personId = person.getPersonId();
            User user = new User();
            user.setPersonId(personId);
            user.setUserName(num);
            user.setPassword(encoder.encode("123456"));
            user.setUserType(userTypeRepository.findByName(EUserType.ROLE_STUDENT.name()));
            user.setCreateTime(DateTimeTool.parseDateTime(new Date()));
            user.setCreatorId(CommonMethod.getPersonId());
            userRepository.saveAndFlush(user);

            student = new Student();
            student.setPersonId(personId);
            studentRepository.saveAndFlush(student);
            isNew = true;
        } else {
            person = student.getPerson();
        }

        personId = person.getPersonId();
        if (!num.equals(person.getNum())) {
            userRepository.findByPersonPersonId(personId).ifPresent(existingUser -> {
                existingUser.setUserName(num);
                userRepository.saveAndFlush(existingUser);
            });
            person.setNum(num);
        }

        person.setName(CommonMethod.getString(form, "name"));
        person.setDept(CommonMethod.getString(form, "dept"));
        person.setCard(CommonMethod.getString(form, "card"));
        person.setGender(CommonMethod.getString(form, "gender"));
        person.setBirthday(CommonMethod.getString(form, "birthday"));
        person.setEmail(CommonMethod.getString(form, "email"));
        person.setPhone(CommonMethod.getString(form, "phone"));
        person.setAddress(CommonMethod.getString(form, "address"));
        person.setIntroduce(CommonMethod.getString(form, "introduce"));
        personRepository.save(person);

        student.setMajor(CommonMethod.getString(form, "major"));
        student.setClassName(CommonMethod.getString(form, "className"));
        studentRepository.save(student);
        systemService.modifyLog(student, isNew);
        return CommonMethod.getReturnData(student.getPersonId());
    }

    public List<Map<String, Object>> getStudentScoreList(List<Score> scoreList) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (scoreList == null || scoreList.isEmpty()) {
            return result;
        }
        for (Score score : scoreList) {
            Map<String, Object> row = new HashMap<>();
            Course course = score.getCourse();
            row.put("studentNum", score.getStudent().getPerson().getNum());
            row.put("scoreId", score.getScoreId());
            row.put("courseNum", course.getNum());
            row.put("courseName", course.getName());
            row.put("credit", course.getCredit());
            row.put("mark", score.getMark());
            row.put("ranking", score.getRanking());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getStudentMarkList(List<Score> scoreList) {
        String[] titles = {"优", "良", "中", "及格", "不及格"};
        int[] count = new int[5];
        List<Map<String, Object>> result = new ArrayList<>();
        if (scoreList == null || scoreList.isEmpty()) {
            return result;
        }
        for (Score score : scoreList) {
            if (score.getMark() >= 90) {
                count[0]++;
            } else if (score.getMark() >= 80) {
                count[1]++;
            } else if (score.getMark() >= 70) {
                count[2]++;
            } else if (score.getMark() >= 60) {
                count[3]++;
            } else {
                count[4]++;
            }
        }
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", titles[i]);
            row.put("title", titles[i]);
            row.put("value", count[i]);
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getStudentFeeList(Integer personId) {
        List<Fee> feeList = feeRepository.findListByStudent(personId);
        List<Map<String, Object>> result = new ArrayList<>();
        if (feeList == null || feeList.isEmpty()) {
            return result;
        }
        for (Fee fee : feeList) {
            Map<String, Object> row = new HashMap<>();
            row.put("title", fee.getDay());
            row.put("value", fee.getMoney());
            result.add(row);
        }
        return result;
    }

    public String importFeeData(Integer personId, InputStream inputStream) {
        try {
            Student student = studentRepository.findById(personId).orElseThrow();
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell dayCell = row.getCell(0);
                if (dayCell == null) {
                    break;
                }
                String day = dayCell.getStringCellValue();
                Cell moneyCell = row.getCell(1);
                String money = moneyCell == null ? null : moneyCell.getStringCellValue();
                Fee fee = feeRepository.findByStudentPersonIdAndDay(personId, day).orElseGet(Fee::new);
                fee.setDay(day);
                fee.setStudent(student);
                fee.setMoney(money != null && !money.isEmpty() ? Double.parseDouble(money) : 0d);
                feeRepository.save(fee);
            }
            workbook.close();
            return null;
        } catch (Exception exception) {
            log.error("Import fee data failed", exception);
            return "消费数据导入失败！";
        }
    }

    public DataResponse importFeeData(@RequestBody byte[] bytes, String personIdStr) {
        Integer personId = Integer.parseInt(personIdStr);
        String message = importFeeData(personId, new ByteArrayInputStream(bytes));
        if (message == null) {
            return CommonMethod.getReturnMessageOK();
        }
        return CommonMethod.getReturnMessageError(message);
    }

    public ResponseEntity<StreamingResponseBody> getStudentListExcl(DataRequest dataRequest) {
        List<Map<String, Object>> list = getStudentMapList(dataRequest.getString("numName"));
        Integer[] widths = {8, 20, 10, 15, 15, 15, 25, 10, 15, 30, 20, 30};
        String[] titles = {"序号", "学号", "姓名", "学院", "专业", "班级", "证件号码", "性别", "出生日期", "邮箱", "电话", "地址"};

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("student.xlsx");
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        XSSFCellStyle style = CommonMethod.createCellStyle(workbook, 11);
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            XSSFCell headerCell = headerRow.createCell(i);
            headerCell.setCellStyle(style);
            headerCell.setCellValue(titles[i]);
        }

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> rowData = list.get(i);
            XSSFRow row = sheet.createRow(i + 1);
            XSSFCell[] cells = new XSSFCell[widths.length];
            for (int j = 0; j < widths.length; j++) {
                cells[j] = row.createCell(j);
                cells[j].setCellStyle(style);
            }
            cells[0].setCellValue(String.valueOf(i + 1));
            cells[1].setCellValue(CommonMethod.getString(rowData, "num"));
            cells[2].setCellValue(CommonMethod.getString(rowData, "name"));
            cells[3].setCellValue(CommonMethod.getString(rowData, "dept"));
            cells[4].setCellValue(CommonMethod.getString(rowData, "major"));
            cells[5].setCellValue(CommonMethod.getString(rowData, "className"));
            cells[6].setCellValue(CommonMethod.getString(rowData, "card"));
            cells[7].setCellValue(CommonMethod.getString(rowData, "genderName"));
            cells[8].setCellValue(CommonMethod.getString(rowData, "birthday"));
            cells[9].setCellValue(CommonMethod.getString(rowData, "email"));
            cells[10].setCellValue(CommonMethod.getString(rowData, "phone"));
            cells[11].setCellValue(CommonMethod.getString(rowData, "address"));
        }

        try {
            StreamingResponseBody stream = workbook::write;
            return ResponseEntity.ok().contentType(CommonMethod.exelType).body(stream);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    public DataResponse getStudentPageData(DataRequest dataRequest) {
        String numName = dataRequest.getString("numName");
        Integer currentPage = dataRequest.getCurrentPage();
        int pageSize = 40;
        int dataTotal = 0;
        List<Map<String, Object>> dataList = new ArrayList<>();
        Pageable pageable = PageRequest.of(currentPage, pageSize);
        Page<Student> page = studentRepository.findStudentPageByNumName(numName, pageable);
        if (page != null) {
            dataTotal = (int) page.getTotalElements();
            for (Student student : page.getContent()) {
                dataList.add(getMapFromStudent(student));
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("dataTotal", dataTotal);
        data.put("pageSize", pageSize);
        data.put("dataList", dataList);
        return CommonMethod.getReturnData(data);
    }

    public DataResponse getFamilyMemberList(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        List<FamilyMember> familyMemberList = familyMemberRepository.findByStudentPersonId(personId);
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (familyMemberList != null) {
            for (FamilyMember familyMember : familyMemberList) {
                Map<String, Object> row = new HashMap<>();
                row.put("memberId", familyMember.getMemberId());
                row.put("personId", familyMember.getStudent().getPersonId());
                row.put("relation", familyMember.getRelation());
                row.put("name", familyMember.getName());
                row.put("gender", familyMember.getGender());
                row.put("age", familyMember.getAge() == null ? "" : familyMember.getAge().toString());
                row.put("unit", familyMember.getUnit());
                dataList.add(row);
            }
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse familyMemberSave(DataRequest dataRequest) {
        Map<String, Object> form = dataRequest.getMap("form");
        Integer personId = CommonMethod.getInteger(form, "personId");
        Integer memberId = CommonMethod.getInteger(form, "memberId");
        FamilyMember familyMember = null;
        if (memberId != null) {
            familyMember = familyMemberRepository.findById(memberId).orElse(null);
        }
        if (familyMember == null) {
            familyMember = new FamilyMember();
            if (personId != null) {
                familyMember.setStudent(studentRepository.findById(personId).orElseThrow());
            }
        }
        familyMember.setRelation(CommonMethod.getString(form, "relation"));
        familyMember.setName(CommonMethod.getString(form, "name"));
        familyMember.setGender(CommonMethod.getString(form, "gender"));
        familyMember.setAge(CommonMethod.getInteger(form, "age"));
        familyMember.setUnit(CommonMethod.getString(form, "unit"));
        familyMemberRepository.save(familyMember);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse familyMemberDelete(DataRequest dataRequest) {
        Integer memberId = dataRequest.getInteger("memberId");
        if (memberId != null) {
            familyMemberRepository.findById(memberId).ifPresent(familyMemberRepository::delete);
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse importFeeDataWeb(Map<String, Object> request, MultipartFile file) {
        Integer personId = CommonMethod.getInteger(request, "personId");
        try {
            String message = importFeeData(personId, file.getInputStream());
            if (message == null) {
                return CommonMethod.getReturnMessageOK();
            }
            return CommonMethod.getReturnMessageError(message);
        } catch (Exception exception) {
            log.error("Import fee data from web failed", exception);
            return CommonMethod.getReturnMessageError("消费数据导入失败！");
        }
    }

    public DataResponse getStudentIntroduceData(DataRequest dataRequest) {
        Integer personId = dataRequest.getInteger("personId");
        Optional<Student> studentOptional;
        if (personId == null || personId <= 0) {
            studentOptional = studentRepository.findByPersonNum(CommonMethod.getUsername());
        } else {
            studentOptional = studentRepository.findById(personId);
        }
        if (studentOptional.isEmpty()) {
            return CommonMethod.getReturnMessageError("学生不存在！");
        }
        Student student = studentOptional.get();
        List<Score> scoreList = scoreRepository.findByStudentPersonId(student.getPersonId());
        Map<String, Object> data = new HashMap<>();
        data.put("info", getMapFromStudent(student));
        data.put("scoreList", getStudentScoreList(scoreList));
        data.put("markList", getStudentMarkList(scoreList));
        data.put("feeList", getStudentFeeList(student.getPersonId()));
        return CommonMethod.getReturnData(data);
    }
}
