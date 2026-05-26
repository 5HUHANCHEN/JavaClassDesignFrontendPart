package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Integer> {
    Optional<HomeworkSubmission> findByAssignmentHomeworkIdAndStudentPersonId(Integer homeworkId, Integer personId);

    @Query("from HomeworkSubmission s where (?1=0 or s.assignment.homeworkId=?1) and (?2='' or s.student.person.num like %?2% or s.student.person.name like %?2% or s.assignment.title like %?2%) order by s.submissionId desc")
    List<HomeworkSubmission> findSubmissionList(Integer homeworkId, String keyword);

    List<HomeworkSubmission> findByStudentPersonId(Integer personId);

    @Query("from HomeworkSubmission s where s.grade is not null and s.student.personId in ?1")
    List<HomeworkSubmission> findGradedByStudentIds(List<Integer> personIds);
}
