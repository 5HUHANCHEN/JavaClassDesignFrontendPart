package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.RegisterApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisterApplyRepository extends JpaRepository<RegisterApply, Integer> {
    Optional<RegisterApply> findByUsernameAndStatus(String username, Integer status);
    List<RegisterApply> findByStatusOrderByApplyIdDesc(Integer status);
}