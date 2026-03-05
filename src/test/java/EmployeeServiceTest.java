
import ProjectForJob.example.Job.JobApplication;
import ProjectForJob.example.Job.entityJob.EmployeesEntity;
import ProjectForJob.example.Job.repositories.ListEmployeesRepository;
import ProjectForJob.example.Job.services.EmployeesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = JobApplication.class)
@Testcontainers

public class EmployeeServiceTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18.1")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private EmployeesService employeesService;

    @Autowired
    private ListEmployeesRepository repository;

    @BeforeEach
    void setUp() {
        // Очищаем таблицу перед каждым тестом, чтобы они не влияли друг на друга
        repository.deleteAll();
    }


    @Test
    void save_ShouldPersistEmployee() {
        // given
        EmployeesEntity employee = new EmployeesEntity();
        employee.setName("Александер");

        // when
        EmployeesEntity saved = employeesService.save(employee);

        // then
        assertThat(saved.getId()).isNotNull(); // ID должен сгенерироваться
        assertThat(saved.getName()).isEqualTo("Александер");

        // Проверим, что в базе действительно появилась запись
        List<EmployeesEntity> all = employeesService.findAll();
        assertThat(all.size()).isEqualTo(1);
        assertThat(all.get(0).getName()).isEqualTo("Александер");
    }

    @Test
    void findById_ShouldReturnEmployee() {
        EmployeesEntity employee = new EmployeesEntity();
        employee.setName("Константин");

        EmployeesEntity saved = employeesService.save(employee);

        EmployeesEntity found = employeesService.findById(saved.getId());

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Константин");


    }

    @Test
    void findById_ShouldReturnNull_WhenNotExists() {
        // when
        EmployeesEntity found = employeesService.findById(999L);

        // then
        assertThat(found).isNull();
    }
    @Test
    void delete_ShouldRemoveEmployee() {
        // given
        EmployeesEntity employee = new EmployeesEntity();
        employee.setName("To be deleted");
        EmployeesEntity saved = employeesService.save(employee);

        // when
        employeesService.deleteById(saved.getId());

        // then
        assertThat(employeesService.findById(saved.getId())).isNull();
        assertTrue(employeesService.findAll().isEmpty());
    }



}
