package pl.learnedge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.learnedge.model.Course;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonDto {
    private Long id;
    private String title;
    private String content;
    private int lessonOrder;
    private Course course;
    private String slug;
    private boolean completed;
}
