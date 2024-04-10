package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByPath(String path);

    Optional<Page> findById(Integer id);
    @Query("SELECT p.id FROM Page p WHERE p.site.id = :id")
    List<Integer> findIdsBySiteId(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM page WHERE site_id IN (SELECT id FROM site WHERE url IN ?1)", nativeQuery = true)
    void deleteBySite_UrlIn(List<String> urls);
    List<Page> findAllByPath(String url);
    default boolean existsByPathAndSiteId(String path, Integer id){
        for (Page page : this.findAllByPath(path)){
            if (page.getSite().getId() == id){
                return true;
            }
        }
        return false;
    }
    Integer countPagesBySiteId(Integer id);
}
