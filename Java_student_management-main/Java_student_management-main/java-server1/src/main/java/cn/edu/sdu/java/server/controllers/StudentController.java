package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;

/**
 * StudentController 涓昏鏄负瀛︾敓绠＄悊鏁版嵁绠＄悊鎻愪緵鐨刉eb璇锋眰鏈嶅姟
 */

// origins锛?鍏佽鍙闂殑鍩熷垪琛?// maxAge:鍑嗗鍝嶅簲鍓嶇殑缂撳瓨鎸佺画鐨勬渶澶ф椂闂达紙浠ョ涓哄崟浣嶏級銆?@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student")

public class StudentController {
    private final StudentService studentService;
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * getStudentList 瀛︾敓绠＄悊 鐐瑰嚮鏌ヨ鎸夐挳璇锋眰
     * 鍓嶅彴璇锋眰鍙傛暟 numName 瀛﹀彿鎴栧悕绉扮殑 鏌ヨ涓?     * 杩斿洖鍓嶇 瀛樺偍瀛︾敓淇℃伅鐨?MapList 妗嗘灦浼氳嚜鍔ㄥ皢Map杞崲绋嬬敤浜庡墠鍚庡彴浼犺緭鏁版嵁鐨凧son瀵硅薄锛孧ap鐨勫祵濂楃粨鏋勫拰Json鐨勫祵濂楃粨鏋勭被浼?     *
     */


    @PostMapping("/getStudentList")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse getStudentList(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentList(dataRequest);
    }


    /**
     * studentDelete 鍒犻櫎瀛︾敓淇℃伅Web鏈嶅姟 Student椤甸潰鐨勫垪琛ㄩ噷鐐瑰嚮鍒犻櫎鎸夐挳鍒欏彲浠ュ垹闄ゅ凡缁忓瓨鍦ㄧ殑瀛︾敓淇℃伅锛?鍓嶇浼氬皢璇ヨ褰曠殑id 鍥炰紶鍒板悗绔紝鏂规硶浠庡弬鏁拌幏鍙杋d锛屾煡鍑虹浉鍏宠褰曪紝璋冪敤delete鏂规硶鍒犻櫎
     * 杩欓噷娉ㄦ剰鍒犻櫎椤哄簭锛屽簲涓簎ser鍏宠仈person,Student鍏宠仈Person 鎵€浠ヨ鍏堝垹闄tudent,User锛屽啀鍒犻櫎Person
     *
     * @param dataRequest 鍓嶇personId 瑕佸垹闄ょ殑瀛︾敓鐨勪富閿?person_id
     * @return 姝ｅ父鎿嶄綔
     */

    @PostMapping("/studentDelete")
    public DataResponse studentDelete(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.studentDelete(dataRequest);
    }

    /**
     * getStudentInfo 鍓嶇鐐瑰嚮瀛︾敓鍒楄〃鏃跺墠绔幏鍙栧鐢熻缁嗕俊鎭姹傛湇鍔?     *
     * @param dataRequest 浠庡墠绔幏鍙?personId 鏌ヨ瀛︾敓淇℃伅鐨勪富閿?person_id
     * @return 鏍规嵁personId浠庢暟鎹簱涓煡鍑烘暟鎹紝瀛樺湪Map瀵硅薄閲岋紝骞惰繑鍥炲墠绔?     */

    @PostMapping("/getStudentInfo")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse getStudentInfo(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentInfo(dataRequest);
    }

    /**
     * studentEditSave 鍓嶇瀛︾敓淇℃伅鎻愪氦鏈嶅姟
     * 鍓嶇鎶婃墍鏈夋暟鎹墦鍖呮垚涓€涓狫son瀵硅薄浣滀负鍙傛暟浼犲洖鍚庣锛屽悗绔洿鎺ュ彲浠ヨ幏寰楀搴旂殑Map瀵硅薄form, 鍐嶄粠form閲屽彇鍑烘墍鏈夊睘鎬э紝澶嶅埗鍒?     * 瀹炰綋瀵硅薄閲岋紝淇濆瓨鍒版暟鎹簱閲屽嵆鍙紝濡傛灉鏄坊鍔犱竴鏉¤褰曪紝 id 涓虹┖锛岃繖鏄厛 new Person, User,Student 璁＄畻鏂扮殑id锛?澶嶅埗鐩稿叧灞炴€э紝淇濆瓨锛屽鏋滄槸缂栬緫鍘熸潵鐨勪俊鎭紝
     * personId涓嶄负绌恒€傚垯鏌ヨ鍑哄疄浣撳璞★紝澶嶅埗鐩稿叧灞炴€э紝淇濆瓨鍚庝慨鏀规暟鎹簱淇℃伅锛屾案涔呬慨鏀?     *
     * @return 鏂板缓淇敼瀛︾敓鐨勪富閿?student_id 杩斿洖鍓嶇
     */
    @PostMapping("/studentEditSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public DataResponse studentEditSave(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.studentEditSave(dataRequest);
    }



    /**
     * importFeeData 鍓嶇涓婁紶娑堣垂娴佹按Excl琛ㄦ暟鎹湇鍔?     *
     * @param barr         鏂囦欢浜岃繘鍒舵暟鎹?     * @param uploader     涓婁紶鑰?     * @param personIdStr student 涓婚敭
     * @param fileName     鍓嶇涓婁紶鐨勬枃浠跺悕
     */
    @PostMapping(path = "/importFeeData")
    public DataResponse importFeeData(@RequestBody byte[] barr,
                                      @RequestParam(name = "uploader") String uploader,
                                      @RequestParam(name = "personId") String personIdStr,
                                      @RequestParam(name = "fileName") String fileName) {
        return studentService.importFeeData(barr, personIdStr);
    }

    /**
     * getStudentListExcl 鍓嶇涓嬭浇瀵煎嚭瀛︾敓鍩烘湰淇℃伅Excl琛ㄦ暟鎹?     *
     */
    @PostMapping("/getStudentListExcl")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StreamingResponseBody> getStudentListExcl(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentListExcl(dataRequest);
    }


    @PostMapping("/getStudentPageData")
    @PreAuthorize(" hasRole('ADMIN')")
    public DataResponse getStudentPageData(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentPageData(dataRequest);
    }

    /*
        FamilyMember
     */
    @PostMapping("/getFamilyMemberList")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse getFamilyMemberList(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getFamilyMemberList(dataRequest);
    }

    @PostMapping("/familyMemberSave")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse familyMemberSave(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.familyMemberSave(dataRequest);
    }

    @PostMapping("/familyMemberDelete")
    @PreAuthorize(" hasRole('ADMIN') or  hasRole('STUDENT')")
    public DataResponse familyMemberDelete(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.familyMemberDelete(dataRequest);
    }


    @PostMapping("/importFeeDataWeb")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse importFeeDataWeb(@RequestParam Map<String,Object> request, @RequestParam("file") MultipartFile file) {
        return studentService.importFeeDataWeb(request, file);
    }

    @PostMapping("/getStudentIntroduceData")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public DataResponse getStudentIntroduceData(@Valid @RequestBody DataRequest dataRequest) {
        return studentService.getStudentIntroduceData(dataRequest);
    }
}
