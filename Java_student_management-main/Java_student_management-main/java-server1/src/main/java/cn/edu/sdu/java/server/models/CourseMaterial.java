package cn.edu.sdu.java.server.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_material")
public class CourseMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer materialId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private Person uploader;

    @Size(max = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    @Size(max = 200)
    private String fileName;

    @Size(max = 40)
    private String fileType;

    private Long fileSize;

    @Size(max = 20)
    private String uploadTime;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] fileData;
}
