package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework_assignment")
public class HomeworkAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer homeworkId;

    @Size(max = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    @Size(max = 20)
    private String dueDate;

    private Double totalScore;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Size(max = 50)
    private String imageName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Size(max = 20)
    private String createTime;
}
