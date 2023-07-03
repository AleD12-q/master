package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrlAndType(String url, String type);
    Optional<Site> findByNameAndType(String name, String type);
    Integer countByType(String type);
    List<Site> findAllByType(String type);
    @Transactional
    void deleteByType(String type);
}
