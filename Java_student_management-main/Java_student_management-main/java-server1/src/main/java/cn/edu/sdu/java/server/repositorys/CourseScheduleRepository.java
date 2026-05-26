package cn.edu.sdu.java.server.repositorys;

import cn.edu.sdu.java.server.models.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Integer> {
    List<CourseSchedule> findAllByOrderByDayOfWeekAscStartTimeAscStartWeekAsc();
}
