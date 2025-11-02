package pl.learnedge.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private boolean completed;

}
