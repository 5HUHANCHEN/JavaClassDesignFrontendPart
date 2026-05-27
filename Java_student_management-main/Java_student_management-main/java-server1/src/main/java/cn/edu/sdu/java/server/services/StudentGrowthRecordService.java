package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.DictionaryInfo;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentDailyActivity;
import cn.edu.sdu.java.server.models.StudentHonor;
import cn.edu.sdu.java.server.models.StudentInnovationPractice;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.DictionaryInfoRepository;
import cn.edu.sdu.java.server.repositorys.StudentDailyActivityRepository;
import cn.edu.sdu.java.server.repositorys.StudentHonorRepository;
import cn.edu.sdu.java.server.repositorys.StudentInnovationPracticeRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class StudentGrowthRecordService {
    public static final String CATEGORY_INNOVATION = "INNOVATION_PRACTICE";
    public static final String CATEGORY_HONOR = "HONOR";
    public static final String CATEGORY_DAILY = "DAILY_ACTIVITY";

    private static final String TYPE_CODE_INNOVATION = "INNOVATION_PRACTICE_TYPE";
    private static final String TYPE_CODE_HONOR = "STUDENT_HONOR_TYPE";
    private static final String TYPE_CODE_DAILY = "DAILY_ACTIVITY_TYPE";

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            CATEGORY_INNOVATION,
            CATEGORY_HONOR,
            CATEGORY_DAILY
    );

    private final StudentInnovationPracticeRepository studentInnovationPracticeRepository;
    private final StudentHonorRepository studentHonorRepository;
    private final StudentDailyActivityRepository studentDailyActivityRepository;
    private final StudentRepository studentRepository;
    private final DictionaryInfoRepository dictionaryInfoRepository;

    public StudentGrowthRecordService(StudentInnovationPracticeRepository studentInnovationPracticeRepository,
                                      StudentHonorRepository studentHonorRepository,
                                      StudentDailyActivityRepository studentDailyActivityRepository,
                                      StudentRepository studentRepository,
                                      DictionaryInfoRepository dictionaryInfoRepository) {
        this.studentInnovationPracticeRepository = studentInnovationPracticeRepository;
        this.studentHonorRepository = studentHonorRepository;
        this.studentDailyActivityRepository = studentDailyActivityRepository;
        this.studentRepository = studentRepository;
        this.dictionaryInfoRepository = dictionaryInfoRepository;
    }

    public OptionItemList getStudentItemOptionList(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        List<OptionItem> itemList = new ArrayList<>();
        if ("ROLE_STUDENT".equals(roleName)) {
            studentRepository.findByPersonNum(CommonMethod.getUsername()).ifPresent(student ->
                    itemList.add(toStudentOptionItem(student))
            );
            return new OptionItemList(0, itemList);
        }
        List<Student> studentList = studentRepository.findStudentListByNumName("");
        for (Student student : studentList) {
            itemList.add(toStudentOptionItem(student));
        }
        return new OptionItemList(0, itemList);
    }

    public OptionItemList getTypeOptionList(DataRequest dataRequest) {
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return new OptionItemList(1, new ArrayList<>());
        }
        return new OptionItemList(0, getTypeOptionItems(meta));
    }

    public DataResponse getGrowthRecordList(DataRequest dataRequest) {
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return CommonMethod.getReturnMessageError("记录分类不支持！");
        }
        String roleName = CommonMethod.getRoleName();
        String studentNum = "";
        if ("ROLE_STUDENT".equals(roleName)) {
            studentNum = defaultString(CommonMethod.getUsername());
        }
        String search = defaultString(dataRequest.getString("search"));
        Map<Integer, String> typeNameMap = getTypeNameMap(meta);
        List<Map<String, Object>> dataList = switch (meta.categoryCode()) {
            case CATEGORY_INNOVATION -> buildInnovationRecordList(studentNum, search, typeNameMap);
            case CATEGORY_HONOR -> buildHonorRecordList(studentNum, search, typeNameMap);
            case CATEGORY_DAILY -> buildDailyRecordList(studentNum, search, typeNameMap);
            default -> new ArrayList<>();
        };
        return CommonMethod.getReturnData(dataList);
    }

    @Transactional
    public DataResponse saveGrowthRecord(DataRequest dataRequest) {
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return CommonMethod.getReturnMessageError("记录分类不支持！");
        }
        Map<String, Object> form = dataRequest.getMap("form");
        Integer recordId = dataRequest.getInteger("recordId");
        Integer studentId = dataRequest.getInteger("studentId");
        String roleName = CommonMethod.getRoleName();

        Integer itemTypeId = CommonMethod.getInteger(form, "itemTypeId");
        if (!isValidTypeId(meta, itemTypeId)) {
            return CommonMethod.getReturnMessageError("请选择有效的类型！");
        }
        String title = CommonMethod.getString(form, "title").trim();
        if (title.isEmpty()) {
            return CommonMethod.getReturnMessageError("标题不能为空！");
        }

        String startDate = normalizeDateString(CommonMethod.getString(form, "startDate"));
        String endDate = normalizeDateString(CommonMethod.getString(form, "endDate"));
        String dateError = validateDateRange(startDate, endDate);
        if (dateError != null) {
            return CommonMethod.getReturnMessageError(dateError);
        }

        return switch (meta.categoryCode()) {
            case CATEGORY_INNOVATION -> saveInnovationPractice(recordId, studentId, roleName, itemTypeId, form, title, startDate, endDate);
            case CATEGORY_HONOR -> saveStudentHonor(recordId, studentId, roleName, itemTypeId, form, title, startDate, endDate);
            case CATEGORY_DAILY -> saveDailyActivity(recordId, studentId, roleName, itemTypeId, form, title, startDate, endDate);
            default -> CommonMethod.getReturnMessageError("记录分类不支持！");
        };
    }

    @Transactional
    public DataResponse deleteGrowthRecord(DataRequest dataRequest) {
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return CommonMethod.getReturnMessageError("记录分类不支持！");
        }
        Integer recordId = dataRequest.getInteger("recordId");
        if (recordId == null || recordId <= 0) {
            return CommonMethod.getReturnMessageError("记录不存在！");
        }
        return switch (meta.categoryCode()) {
            case CATEGORY_INNOVATION -> deleteInnovationPractice(recordId);
            case CATEGORY_HONOR -> deleteStudentHonor(recordId);
            case CATEGORY_DAILY -> deleteDailyActivity(recordId);
            default -> CommonMethod.getReturnMessageError("记录分类不支持！");
        };
    }

    @Transactional
    public DataResponse saveType(DataRequest dataRequest) {
        if (!"ROLE_ADMIN".equals(CommonMethod.getRoleName())) {
            return CommonMethod.getReturnMessageError("只有管理员可以维护类型！");
        }
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return CommonMethod.getReturnMessageError("记录分类不支持！");
        }
        String typeName = defaultString(dataRequest.getString("typeName")).trim();
        if (typeName.isEmpty()) {
            return CommonMethod.getReturnMessageError("类型名称不能为空！");
        }
        DictionaryInfo root = ensureTypeRoot(meta);
        Integer typeId = dataRequest.getInteger("typeId");
        List<DictionaryInfo> childList = dictionaryInfoRepository.findByPid(root.getId());
        for (DictionaryInfo child : childList) {
            if (Objects.equals(child.getId(), typeId)) {
                continue;
            }
            if (typeName.equals(defaultString(child.getLabel()).trim()) || typeName.equals(defaultString(child.getValue()).trim())) {
                return CommonMethod.getReturnMessageError("类型名称已存在！");
            }
        }

        DictionaryInfo typeInfo;
        if (typeId != null && typeId > 0) {
            typeInfo = dictionaryInfoRepository.findById(typeId).orElse(null);
            if (typeInfo == null || !Objects.equals(root.getId(), typeInfo.getPid())) {
                return CommonMethod.getReturnMessageError("类型不存在！");
            }
        } else {
            typeInfo = new DictionaryInfo();
            typeInfo.setPid(root.getId());
        }
        typeInfo.setValue(typeName);
        typeInfo.setLabel(typeName);
        dictionaryInfoRepository.save(typeInfo);
        return CommonMethod.getReturnData(typeInfo.getId(), "类型保存成功！");
    }

    @Transactional
    public DataResponse deleteType(DataRequest dataRequest) {
        if (!"ROLE_ADMIN".equals(CommonMethod.getRoleName())) {
            return CommonMethod.getReturnMessageError("只有管理员可以维护类型！");
        }
        CategoryMeta meta = getCategoryMeta(dataRequest.getString("category"));
        if (meta == null) {
            return CommonMethod.getReturnMessageError("记录分类不支持！");
        }
        Integer typeId = dataRequest.getInteger("typeId");
        if (typeId == null || typeId <= 0) {
            return CommonMethod.getReturnMessageError("类型不存在！");
        }
        DictionaryInfo root = ensureTypeRoot(meta);
        DictionaryInfo typeInfo = dictionaryInfoRepository.findById(typeId).orElse(null);
        if (typeInfo == null || !Objects.equals(root.getId(), typeInfo.getPid())) {
            return CommonMethod.getReturnMessageError("类型不存在！");
        }
        long usedCount = switch (meta.categoryCode()) {
            case CATEGORY_INNOVATION -> studentInnovationPracticeRepository.countByItemTypeId(typeId);
            case CATEGORY_HONOR -> studentHonorRepository.countByItemTypeId(typeId);
            case CATEGORY_DAILY -> studentDailyActivityRepository.countByItemTypeId(typeId);
            default -> 0L;
        };
        if (usedCount > 0) {
            return CommonMethod.getReturnMessageError("该类型已被记录使用，无法删除！");
        }
        dictionaryInfoRepository.delete(typeInfo);
        return CommonMethod.getReturnMessageOK("类型删除成功！");
    }

    private List<Map<String, Object>> buildInnovationRecordList(String studentNum, String search, Map<Integer, String> typeNameMap) {
        List<StudentInnovationPractice> recordList = studentInnovationPracticeRepository.findRecordList(studentNum, search);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (StudentInnovationPractice record : recordList) {
            dataList.add(toGrowthRecordMap(
                    record.getRecordId(),
                    record.getStudent(),
                    record.getItemTypeId(),
                    typeNameMap,
                    record.getTitle(),
                    record.getLevel(),
                    record.getOrganization(),
                    record.getStartDate(),
                    record.getEndDate(),
                    record.getPlace(),
                    record.getResult(),
                    record.getDescription()
            ));
        }
        return dataList;
    }

    private List<Map<String, Object>> buildHonorRecordList(String studentNum, String search, Map<Integer, String> typeNameMap) {
        List<StudentHonor> recordList = studentHonorRepository.findRecordList(studentNum, search);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (StudentHonor record : recordList) {
            dataList.add(toGrowthRecordMap(
                    record.getRecordId(),
                    record.getStudent(),
                    record.getItemTypeId(),
                    typeNameMap,
                    record.getTitle(),
                    record.getLevel(),
                    record.getOrganization(),
                    record.getStartDate(),
                    record.getEndDate(),
                    "",
                    record.getResult(),
                    record.getDescription()
            ));
        }
        return dataList;
    }

    private List<Map<String, Object>> buildDailyRecordList(String studentNum, String search, Map<Integer, String> typeNameMap) {
        List<StudentDailyActivity> recordList = studentDailyActivityRepository.findRecordList(studentNum, search);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (StudentDailyActivity record : recordList) {
            dataList.add(toGrowthRecordMap(
                    record.getRecordId(),
                    record.getStudent(),
                    record.getItemTypeId(),
                    typeNameMap,
                    record.getTitle(),
                    record.getLevel(),
                    record.getOrganization(),
                    record.getStartDate(),
                    record.getEndDate(),
                    record.getPlace(),
                    record.getResult(),
                    record.getDescription()
            ));
        }
        return dataList;
    }

    private DataResponse saveInnovationPractice(Integer recordId, Integer studentId, String roleName, Integer itemTypeId,
                                                Map<String, Object> form, String title, String startDate, String endDate) {
        StudentInnovationPractice record = null;
        if (recordId != null && recordId > 0) {
            record = studentInnovationPracticeRepository.findById(recordId).orElse(null);
            if (record == null) {
                return CommonMethod.getReturnMessageError("记录不存在！");
            }
            if (!canEditStudentRecord(roleName, record.getStudent())) {
                return CommonMethod.getReturnMessageError("只能修改自己的记录！");
            }
        }
        Student student = resolveStudent(roleName, studentId, record == null ? null : record.getStudent());
        if (student == null) {
            return CommonMethod.getReturnMessageError("学生不存在！");
        }
        if (record == null) {
            record = new StudentInnovationPractice();
            record.setCreateTime(new Date());
        }
        fillCommonRecordFields(record, student, itemTypeId, form, title, startDate, endDate, true);
        studentInnovationPracticeRepository.save(record);
        return CommonMethod.getReturnData(record.getRecordId(), "记录保存成功！");
    }

    private DataResponse saveStudentHonor(Integer recordId, Integer studentId, String roleName, Integer itemTypeId,
                                          Map<String, Object> form, String title, String startDate, String endDate) {
        StudentHonor record = null;
        if (recordId != null && recordId > 0) {
            record = studentHonorRepository.findById(recordId).orElse(null);
            if (record == null) {
                return CommonMethod.getReturnMessageError("记录不存在！");
            }
            if (!canEditStudentRecord(roleName, record.getStudent())) {
                return CommonMethod.getReturnMessageError("只能修改自己的记录！");
            }
        }
        Student student = resolveStudent(roleName, studentId, record == null ? null : record.getStudent());
        if (student == null) {
            return CommonMethod.getReturnMessageError("学生不存在！");
        }
        if (record == null) {
            record = new StudentHonor();
            record.setCreateTime(new Date());
        }
        fillCommonRecordFields(record, student, itemTypeId, form, title, startDate, endDate);
        studentHonorRepository.save(record);
        return CommonMethod.getReturnData(record.getRecordId(), "记录保存成功！");
    }

    private DataResponse saveDailyActivity(Integer recordId, Integer studentId, String roleName, Integer itemTypeId,
                                           Map<String, Object> form, String title, String startDate, String endDate) {
        StudentDailyActivity record = null;
        if (recordId != null && recordId > 0) {
            record = studentDailyActivityRepository.findById(recordId).orElse(null);
            if (record == null) {
                return CommonMethod.getReturnMessageError("记录不存在！");
            }
            if (!canEditStudentRecord(roleName, record.getStudent())) {
                return CommonMethod.getReturnMessageError("只能修改自己的记录！");
            }
        }
        Student student = resolveStudent(roleName, studentId, record == null ? null : record.getStudent());
        if (student == null) {
            return CommonMethod.getReturnMessageError("学生不存在！");
        }
        if (record == null) {
            record = new StudentDailyActivity();
            record.setCreateTime(new Date());
        }
        fillCommonRecordFields(record, student, itemTypeId, form, title, startDate, endDate, true);
        studentDailyActivityRepository.save(record);
        return CommonMethod.getReturnData(record.getRecordId(), "记录保存成功！");
    }

    private DataResponse deleteInnovationPractice(Integer recordId) {
        StudentInnovationPractice record = studentInnovationPracticeRepository.findById(recordId).orElse(null);
        if (record == null) {
            return CommonMethod.getReturnMessageError("记录不存在！");
        }
        if (!canDeleteStudentRecord(record.getStudent())) {
            return CommonMethod.getReturnMessageError("只能删除自己的记录！");
        }
        studentInnovationPracticeRepository.delete(record);
        return CommonMethod.getReturnMessageOK("记录删除成功！");
    }

    private DataResponse deleteStudentHonor(Integer recordId) {
        StudentHonor record = studentHonorRepository.findById(recordId).orElse(null);
        if (record == null) {
            return CommonMethod.getReturnMessageError("记录不存在！");
        }
        if (!canDeleteStudentRecord(record.getStudent())) {
            return CommonMethod.getReturnMessageError("只能删除自己的记录！");
        }
        studentHonorRepository.delete(record);
        return CommonMethod.getReturnMessageOK("记录删除成功！");
    }

    private DataResponse deleteDailyActivity(Integer recordId) {
        StudentDailyActivity record = studentDailyActivityRepository.findById(recordId).orElse(null);
        if (record == null) {
            return CommonMethod.getReturnMessageError("记录不存在！");
        }
        if (!canDeleteStudentRecord(record.getStudent())) {
            return CommonMethod.getReturnMessageError("只能删除自己的记录！");
        }
        studentDailyActivityRepository.delete(record);
        return CommonMethod.getReturnMessageOK("记录删除成功！");
    }

    private void fillCommonRecordFields(StudentInnovationPractice record, Student student, Integer itemTypeId,
                                        Map<String, Object> form, String title, String startDate, String endDate,
                                        boolean includePlace) {
        record.setStudent(student);
        record.setItemTypeId(itemTypeId);
        record.setTitle(title);
        record.setLevel(CommonMethod.getString(form, "level").trim());
        record.setOrganization(CommonMethod.getString(form, "organization").trim());
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setPlace(includePlace ? CommonMethod.getString(form, "place").trim() : "");
        record.setResult(CommonMethod.getString(form, "result").trim());
        record.setDescription(CommonMethod.getString(form, "description").trim());
        record.setUpdateTime(new Date());
    }

    private void fillCommonRecordFields(StudentDailyActivity record, Student student, Integer itemTypeId,
                                        Map<String, Object> form, String title, String startDate, String endDate,
                                        boolean includePlace) {
        record.setStudent(student);
        record.setItemTypeId(itemTypeId);
        record.setTitle(title);
        record.setLevel(CommonMethod.getString(form, "level").trim());
        record.setOrganization(CommonMethod.getString(form, "organization").trim());
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setPlace(includePlace ? CommonMethod.getString(form, "place").trim() : "");
        record.setResult(CommonMethod.getString(form, "result").trim());
        record.setDescription(CommonMethod.getString(form, "description").trim());
        record.setUpdateTime(new Date());
    }

    private void fillCommonRecordFields(StudentHonor record, Student student, Integer itemTypeId,
                                        Map<String, Object> form, String title, String startDate, String endDate) {
        record.setStudent(student);
        record.setItemTypeId(itemTypeId);
        record.setTitle(title);
        record.setLevel(CommonMethod.getString(form, "level").trim());
        record.setOrganization(CommonMethod.getString(form, "organization").trim());
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setResult(CommonMethod.getString(form, "result").trim());
        record.setDescription(CommonMethod.getString(form, "description").trim());
        record.setUpdateTime(new Date());
    }

    private boolean canEditStudentRecord(String roleName, Student student) {
        if (!"ROLE_STUDENT".equals(roleName)) {
            return true;
        }
        return student != null
                && student.getPerson() != null
                && Objects.equals(student.getPerson().getNum(), CommonMethod.getUsername());
    }

    private boolean canDeleteStudentRecord(Student student) {
        if (!"ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            return true;
        }
        return student != null
                && student.getPerson() != null
                && Objects.equals(student.getPerson().getNum(), CommonMethod.getUsername());
    }

    private Student resolveStudent(String roleName, Integer studentId, Student currentStudent) {
        if ("ROLE_STUDENT".equals(roleName)) {
            return studentRepository.findByPersonNum(CommonMethod.getUsername()).orElse(null);
        }
        if (studentId != null && studentId > 0) {
            Optional<Student> studentOptional = studentRepository.findById(studentId);
            if (studentOptional.isPresent()) {
                return studentOptional.get();
            }
        }
        return currentStudent;
    }

    private Map<String, Object> toGrowthRecordMap(Integer recordId, Student student, Integer itemTypeId,
                                                  Map<Integer, String> typeNameMap, String title, String level,
                                                  String organization, String startDate, String endDate, String place,
                                                  String result, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("recordId", recordId);
        map.put("studentId", student == null ? null : student.getPersonId());
        map.put("studentNum", student == null || student.getPerson() == null ? "" : student.getPerson().getNum());
        map.put("studentName", student == null || student.getPerson() == null ? "" : student.getPerson().getName());
        map.put("itemTypeId", itemTypeId);
        map.put("itemType", defaultString(typeNameMap.get(itemTypeId)));
        map.put("title", defaultString(title));
        map.put("level", defaultString(level));
        map.put("organization", defaultString(organization));
        map.put("startDate", defaultString(startDate));
        map.put("endDate", defaultString(endDate));
        map.put("dateRange", buildDateRange(startDate, endDate));
        map.put("place", defaultString(place));
        map.put("result", defaultString(result));
        map.put("description", defaultString(description));
        return map;
    }

    private Map<Integer, String> getTypeNameMap(CategoryMeta meta) {
        Map<Integer, String> typeNameMap = new HashMap<>();
        for (DictionaryInfo info : getTypeDictionaryList(meta)) {
            typeNameMap.put(info.getId(), getDictionaryDisplayName(info));
        }
        return typeNameMap;
    }

    private List<OptionItem> getTypeOptionItems(CategoryMeta meta) {
        List<OptionItem> itemList = new ArrayList<>();
        for (DictionaryInfo info : getTypeDictionaryList(meta)) {
            itemList.add(new OptionItem(info.getId(), String.valueOf(info.getId()), getDictionaryDisplayName(info)));
        }
        return itemList;
    }

    private List<DictionaryInfo> getTypeDictionaryList(CategoryMeta meta) {
        DictionaryInfo root = ensureTypeRoot(meta);
        List<DictionaryInfo> typeList = new ArrayList<>(dictionaryInfoRepository.findByPid(root.getId()));
        typeList.sort(Comparator.comparing(DictionaryInfo::getId));
        return typeList;
    }

    private boolean isValidTypeId(CategoryMeta meta, Integer itemTypeId) {
        if (itemTypeId == null || itemTypeId <= 0) {
            return false;
        }
        DictionaryInfo root = ensureTypeRoot(meta);
        DictionaryInfo info = dictionaryInfoRepository.findById(itemTypeId).orElse(null);
        return info != null && Objects.equals(root.getId(), info.getPid());
    }

    private DictionaryInfo ensureTypeRoot(CategoryMeta meta) {
        List<DictionaryInfo> rootList = dictionaryInfoRepository.findRootList();
        DictionaryInfo root = null;
        for (DictionaryInfo item : rootList) {
            if (meta.typeCode().equals(item.getValue())) {
                root = item;
                break;
            }
        }
        if (root == null) {
            root = new DictionaryInfo();
            root.setPid(0);
            root.setValue(meta.typeCode());
            root.setLabel(meta.typeLabel());
            root = dictionaryInfoRepository.save(root);
        }

        List<DictionaryInfo> childList = dictionaryInfoRepository.findByPid(root.getId());
        Set<String> existingSet = new LinkedHashSet<>();
        for (DictionaryInfo child : childList) {
            existingSet.add(defaultString(child.getValue()).trim());
            existingSet.add(defaultString(child.getLabel()).trim());
        }
        for (String defaultType : meta.defaultTypeList()) {
            if (existingSet.contains(defaultType)) {
                continue;
            }
            DictionaryInfo child = new DictionaryInfo();
            child.setPid(root.getId());
            child.setValue(defaultType);
            child.setLabel(defaultType);
            dictionaryInfoRepository.save(child);
        }
        return root;
    }

    private String getDictionaryDisplayName(DictionaryInfo info) {
        String label = defaultString(info.getLabel()).trim();
        if (!label.isEmpty()) {
            return label;
        }
        return defaultString(info.getValue()).trim();
    }

    private CategoryMeta getCategoryMeta(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim().toUpperCase();
        if (!SUPPORTED_CATEGORIES.contains(normalized)) {
            return null;
        }
        return switch (normalized) {
            case CATEGORY_INNOVATION -> new CategoryMeta(
                    CATEGORY_INNOVATION,
                    TYPE_CODE_INNOVATION,
                    "创新实践类型",
                    List.of("社会实践", "学科竞赛", "科技成果", "培训讲座", "创新项目", "校外实习")
            );
            case CATEGORY_HONOR -> new CategoryMeta(
                    CATEGORY_HONOR,
                    TYPE_CODE_HONOR,
                    "学生荣誉类型",
                    List.of("荣誉称号", "奖学金", "竞赛奖励", "先进个人", "证书奖励")
            );
            case CATEGORY_DAILY -> new CategoryMeta(
                    CATEGORY_DAILY,
                    TYPE_CODE_DAILY,
                    "日常活动类型",
                    List.of("体育活动", "外出旅游", "文艺演出", "聚会活动", "志愿活动")
            );
            default -> null;
        };
    }

    private String normalizeDateString(String value) {
        return defaultString(value).trim();
    }

    private String validateDateRange(String startDate, String endDate) {
        try {
            LocalDate start = startDate.isEmpty() ? null : LocalDate.parse(startDate);
            LocalDate end = endDate.isEmpty() ? null : LocalDate.parse(endDate);
            if (start != null && end != null && start.isAfter(end)) {
                return "开始日期不能晚于结束日期！";
            }
            return null;
        } catch (DateTimeParseException e) {
            return "日期格式不正确，请使用 yyyy-MM-dd ！";
        }
    }

    private OptionItem toStudentOptionItem(Student student) {
        return new OptionItem(
                student.getPersonId(),
                String.valueOf(student.getPersonId()),
                student.getPerson().getNum() + "-" + student.getPerson().getName()
        );
    }

    private String buildDateRange(String startDate, String endDate) {
        String start = defaultString(startDate);
        String end = defaultString(endDate);
        if (start.isEmpty() && end.isEmpty()) {
            return "";
        }
        if (end.isEmpty()) {
            return start;
        }
        if (start.isEmpty()) {
            return end;
        }
        return start + " ~ " + end;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private record CategoryMeta(String categoryCode, String typeCode, String typeLabel, List<String> defaultTypeList) {
    }
}
