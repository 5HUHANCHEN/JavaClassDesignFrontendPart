package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.StudentInnovationPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentInnovationPracticeRepository extends JpaRepository<StudentInnovationPractice, Integer> {
    @Query("""
            from StudentInnovationPractice record
            where (?1 = '' or record.student.person.num = ?1)
              and (?2 = '' or record.student.person.num like %?2%
                   or record.student.person.name like %?2%
                   or record.title like %?2%
                   or record.organization like %?2%
                   or record.result like %?2%
                   or record.description like %?2%)
            order by record.startDate desc, record.recordId desc
            """)
    List<StudentInnovationPractice> findRecordList(String studentNum, String search);

    long countByItemTypeId(Integer itemTypeId);
}
