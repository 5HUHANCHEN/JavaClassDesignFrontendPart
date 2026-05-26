package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Integer> {
    @Query("""
            from CourseMaterial m
            where (:courseId = 0 or m.course.courseId = :courseId)
              and (:keyword = '' or m.title like %:keyword%
                   or m.description like %:keyword%
                   or m.fileName like %:keyword%
                   or m.course.name like %:keyword%
                   or m.uploader.name like %:keyword%)
            order by m.materialId desc
            """)
    List<CourseMaterial> findMaterialList(@Param("courseId") Integer courseId, @Param("keyword") String keyword);

    List<CourseMaterial> findByCourseCourseIdOrderByMaterialIdDesc(Integer courseId);
}
