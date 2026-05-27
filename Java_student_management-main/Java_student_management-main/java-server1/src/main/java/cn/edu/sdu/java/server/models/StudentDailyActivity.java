package cn.edu.sdu.java.server.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "student_daily_activity")
public class StudentDailyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    private Integer itemTypeId;

    @Size(max = 120)
    private String title;

    @Size(max = 60)
    private String level;

    @Size(max = 120)
    private String organization;

    @Size(max = 30)
    private String startDate;

    @Size(max = 30)
    private String endDate;

    @Size(max = 120)
    private String place;

    @Size(max = 200)
    private String result;

    @Size(max = 500)
    private String description;

    private Date createTime;

    private Date updateTime;
}
