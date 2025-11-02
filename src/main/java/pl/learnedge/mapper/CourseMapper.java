package pl.learnedge.mapper;

import org.springframework.stereotype.Component;
import pl.learnedge.dto.CourseDto;
import pl.learnedge.dto.LessonDto;
import pl.learnedge.model.Course;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.UserCourse;

import java.util.List;

@Component
public class CourseMapper {

    // Enrolled Course to User
    public CourseDto toDto(UserCourse userCourse) {
        Course course = userCourse.getCourse();
        CourseDto courseDto = new CourseDto();
        courseDto.setId(course.getId());
        courseDto.setName(course.getName());
        courseDto.setDescription(course.getDescription());
        courseDto.setDifficulty(course.getDifficulty());
        courseDto.setProgress(userCourse.getProgress());
        courseDto.setSlug(course.getSlug());
        return courseDto;
    }

    // Not enrolled courses to user
    public CourseDto toDto(Course course) {
        CourseDto courseDto = new CourseDto();
        courseDto.setId(course.getId());
        courseDto.setName(course.getName());
        courseDto.setDescription(course.getDescription());
        courseDto.setDifficulty(course.getDifficulty());
        courseDto.setProgress(null);
        courseDto.setSlug(course.getSlug());
        return courseDto;
    }

    // Return course with assigned lesson
    public CourseDto toDto(Course course, List<LessonDto> lessons) {
        CourseDto courseDto = new CourseDto();
        courseDto.setId(course.getId());
        courseDto.setName(course.getName());
        courseDto.setDescription(course.getDescription());
        courseDto.setDifficulty(course.getDifficulty());
        courseDto.setProgress(null);
        courseDto.setSlug(course.getSlug());
        courseDto.setLessons(lessons);
        return courseDto;

    }

    public Course toEntity(CourseDto courseDto) {
        Course course = new Course();
        course.setName(courseDto.getName());
        course.setDescription(courseDto.getDescription());
        course.setDifficulty(courseDto.getDifficulty());
        course.setSlug(courseDto.getSlug());
        return course;
    }



}
