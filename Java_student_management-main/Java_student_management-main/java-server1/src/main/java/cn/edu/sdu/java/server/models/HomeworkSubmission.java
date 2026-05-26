package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework_submission",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"homework_id", "student_id"})
        })
public class HomeworkSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer submissionId;

    @ManyToOne
    @JoinColumn(name = "homework_id")
    private HomeworkAssignment assignment;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(length = 2000)
    private String content;

    @Size(max = 20)
    private String submitTime;

    private Double grade;

    @Column(length = 1000)
    private String teacherComment;

    @Size(max = 20)
    private String state;

    @Size(max = 50)
    private String imageName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;
}
