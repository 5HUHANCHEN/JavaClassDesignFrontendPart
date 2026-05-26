package cn.edu.sdu.java.server.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_schedule")
public class CourseSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer courseId;

    @Size(max = 100)
    private String name;

    private Integer dayOfWeek;

    @Size(max = 20)
    private String startTime;

    private Integer startWeek;

    private Integer stopWeek;
}
