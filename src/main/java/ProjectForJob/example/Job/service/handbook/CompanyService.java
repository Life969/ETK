package ProjectForJob.example.Job.services.Handbook;

import ProjectForJob.example.Job.repository.entity.order.CompanyEntity;
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
