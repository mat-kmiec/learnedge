package pl.learnedge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.learnedge.dto.CourseDto;
import pl.learnedge.dto.LessonDto;
import pl.learnedge.exception.CourseNotFoundException;
import pl.learnedge.exception.UserAlreadyEnrollException;
import pl.learnedge.exception.UserNotFoundException;
import pl.learnedge.mapper.CourseMapper;
import pl.learnedge.mapper.LessonMapper;
import pl.learnedge.model.Course;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.User;
import pl.learnedge.model.UserCourse;
import pl.learnedge.repository.*;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final UserCourseRepository userCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public List<CourseDto> getAvailableCoursesForUser(Long userId){
        return courseRepository.findAllCoursesNotEnrolledByUser(userId)
                .stream()
                .map(courseMapper::toDto)
                .toList();
   }

   public List<CourseDto> getEnrolledCoursesForUser(Long userId){
        return userCourseRepository.findAllByUserId(userId)
                .stream()
                .map(courseMapper::toDto)
                .toList();
    }


    @Transactional
    public CourseDto getCourseBySlug(String slug, Long userId) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(CourseNotFoundException::new);

        List<Lesson> lessons = lessonRepository.findAllByCourseId(course.getId());


        List<Long> completedLessonIds = lessonProgressRepository
                .findCompletedLessonIdsByCourseIdAndUserId(course.getId(), userId);

        List<LessonDto> lessonDtos = lessons.stream()
                .map(lesson -> {
                    LessonDto dto = lessonMapper.toDto(lesson);

                    boolean isCompleted = completedLessonIds.contains(lesson.getId());
                    dto.setCompleted(isCompleted);

                    return dto;
                })
                .toList();

        return courseMapper.toDto(course, lessonDtos);
    }


    @Transactional
    public void enrollUserForCourse(Long courseId, Long userId) {
       boolean alreadyEnrolled = userCourseRepository.existsByUserIdAndCourseId(userId, courseId);
       if (alreadyEnrolled){
           throw new UserAlreadyEnrollException();
       }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

       Course course = courseRepository.findById(courseId)
               .orElseThrow(CourseNotFoundException::new);

       UserCourse userCourse = UserCourse.builder()
               .user(user)
               .course(course)
               .progress(0)
               .build();

       userCourseRepository.save(userCourse);
    }
}
