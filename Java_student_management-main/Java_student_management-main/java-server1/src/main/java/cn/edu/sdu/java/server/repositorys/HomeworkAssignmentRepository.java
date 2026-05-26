package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.HomeworkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, Integer> {
    @Query("from HomeworkAssignment h where ?1='' or h.title like %?1% or h.description like %?1% order by h.homeworkId desc")
    List<HomeworkAssignment> findByKeyword(String keyword);
}
