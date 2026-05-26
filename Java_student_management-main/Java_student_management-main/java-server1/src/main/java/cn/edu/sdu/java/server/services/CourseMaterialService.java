package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseMaterial;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CourseMaterialRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.PersonRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CourseMaterialService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseRepository courseRepository;
    private final PersonRepository personRepository;

    public CourseMaterialService(CourseMaterialRepository courseMaterialRepository,
                                 CourseRepository courseRepository,
                                 PersonRepository personRepository) {
        this.courseMaterialRepository = courseMaterialRepository;
        this.courseRepository = courseRepository;
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public DataResponse getMaterialList(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String keyword = dataRequest.getString("keyword");
        List<CourseMaterial> materials = courseMaterialRepository.findMaterialList(courseId == null ? 0 : courseId, keyword == null ? "" : keyword.trim());
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (CourseMaterial material : materials) {
            dataList.add(getMaterialMap(material));
        }
        return CommonMethod.getReturnData(dataList);
    }

    @Transactional
    public DataResponse materialSave(DataRequest dataRequest) {
        Integer courseId = dataRequest.getInteger("courseId");
        String title = trimToEmpty(dataRequest.getString("title"));
        if (courseId == null) {
            return CommonMethod.getReturnMessageError("请选择课程。");
        }
        if (title.isBlank()) {
            return CommonMethod.getReturnMessageError("资料标题不能为空。");
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return CommonMethod.getReturnMessageError("课程不存在。");
        }
        Integer materialId = dataRequest.getInteger("materialId");
        CourseMaterial material = materialId == null
                ? new CourseMaterial()
                : courseMaterialRepository.findById(materialId).orElse(new CourseMaterial());
        material.setCourse(course);
        material.setTitle(title);
        material.setDescription(trimToEmpty(dataRequest.getString("description")));
        if (material.getUploader() == null) {
            getCurrentPerson().ifPresent(material::setUploader);
        }
        if (material.getUploadTime() == null || material.getUploadTime().isBlank()) {
            material.setUploadTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        }
        courseMaterialRepository.save(material);
        return CommonMethod.getReturnData(getMaterialMap(material), "资料保存成功。");
    }

    @Transactional
    public DataResponse materialDelete(DataRequest dataRequest) {
        Integer materialId = dataRequest.getInteger("materialId");
        if (materialId == null) {
            return CommonMethod.getReturnMessageError("请选择要删除的资料。");
        }
        if (!courseMaterialRepository.existsById(materialId)) {
            return CommonMethod.getReturnMessageError("资料不存在。");
        }
        courseMaterialRepository.deleteById(materialId);
        return CommonMethod.getReturnMessageOK("资料删除成功。");
    }

    @Transactional
    public DataResponse uploadMaterialFile(byte[] data, Integer materialId, String fileName) {
        if (materialId == null) {
            return CommonMethod.getReturnMessageError("请先保存资料记录，再上传文件。");
        }
        if (data == null || data.length == 0) {
            return CommonMethod.getReturnMessageError("上传文件为空。");
        }
        CourseMaterial material = courseMaterialRepository.findById(materialId).orElse(null);
        if (material == null) {
            return CommonMethod.getReturnMessageError("资料不存在。");
        }
        String safeFileName = trimToEmpty(fileName);
        if (safeFileName.isBlank()) {
            safeFileName = "course-material";
        }
        material.setFileData(data);
        material.setFileName(safeFileName);
        material.setFileType(getFileType(safeFileName));
        material.setFileSize((long) data.length);
        material.setUploadTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        getCurrentPerson().ifPresent(material::setUploader);
        courseMaterialRepository.save(material);
        return CommonMethod.getReturnData(getMaterialMap(material), "文件上传成功。");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> downloadMaterialFile(DataRequest dataRequest) {
        Integer materialId = dataRequest.getInteger("materialId");
        CourseMaterial material = materialId == null ? null : courseMaterialRepository.findById(materialId).orElse(null);
        if (material == null || material.getFileData() == null || material.getFileData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = material.getFileData();
        String fileName = material.getFileName() == null || material.getFileName().isBlank()
                ? "course-material"
                : material.getFileName();
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingResponseBody stream = outputStream -> outputStream.write(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentLength(data.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

    public Map<String, Object> getMaterialMap(CourseMaterial material) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("materialId", material.getMaterialId());
        if (material.getCourse() != null) {
            map.put("courseId", material.getCourse().getCourseId());
            map.put("courseNum", material.getCourse().getNum());
            map.put("courseName", material.getCourse().getName());
        }
        if (material.getUploader() != null) {
            map.put("uploaderId", material.getUploader().getPersonId());
            map.put("uploaderName", material.getUploader().getName());
        }
        map.put("title", material.getTitle());
        map.put("description", material.getDescription());
        map.put("fileName", material.getFileName());
        map.put("fileType", material.getFileType());
        map.put("fileSize", material.getFileSize());
        map.put("fileSizeText", formatFileSize(material.getFileSize()));
        map.put("uploadTime", material.getUploadTime());
        map.put("hasFile", material.getFileData() != null && material.getFileData().length > 0);
        return map;
    }

    private Optional<Person> getCurrentPerson() {
        Integer personId = CommonMethod.getPersonId();
        if (personId == null) {
            return Optional.empty();
        }
        return personRepository.findById(personId);
    }

    private String getFileType(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "FILE";
        }
        return fileName.substring(index + 1).toUpperCase(Locale.ROOT);
    }

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "";
        }
        if (size < 1024) {
            return size + " B";
        }
        double kb = size / 1024d;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024d);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
