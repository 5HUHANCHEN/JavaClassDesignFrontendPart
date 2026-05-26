package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.DivinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivinationRecordRepository extends JpaRepository<DivinationRecord, Integer> {
    List<DivinationRecord> findByUserPersonIdOrderByRecordIdDesc(Integer personId);
}
