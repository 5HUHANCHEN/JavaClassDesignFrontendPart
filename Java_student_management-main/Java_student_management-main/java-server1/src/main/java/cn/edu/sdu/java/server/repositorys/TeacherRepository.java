package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher,Integer> {

    @Query("from Teacher t where ?1='' or t.person.num like %?1% or t.person.name like %?1%")
    List<Teacher> findTeacherListByNumName(String numName);
}