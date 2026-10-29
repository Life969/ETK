package ProjectForJob.example.Job.service.handbook;

import ProjectForJob.example.Job.domain.entity.order.CompanyEntity;
import ProjectForJob.example.Job.repository.order.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CompanyService {
    private final CompanyRepository companyRepository;

    public List<CompanyEntity> findAll() {
        return companyRepository.findAll();
    }
}
