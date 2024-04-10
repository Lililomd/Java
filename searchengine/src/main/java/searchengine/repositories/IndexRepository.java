package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByPageId(Integer pageId);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index` WHERE page_id IN ?1", nativeQuery = true)
    void deleteAllByPageIdIn(List<Integer> pageIds);

    @Query("SELECT DISTINCT i.page.id FROM Index i WHERE i.lemma.id IN (:lemmasIds)")
    List<Integer> findPageIdsByLemmaIds(@Param("lemmasIds") List<Integer> lemmasIds);

    @Query("SELECT DISTINCT i.page.id FROM Index i WHERE i.lemma.id IN (:lemmasIds) AND i.page.id IN (:pageIds)")
    List<Integer> findPageIdsByLemmaIdsAndPageIds(@Param("lemmasIds") List<Integer> lemmasIds, @Param("pageIds") List<Integer> pageIds);

    @Query("SELECT DISTINCT i.rank FROM Index i WHERE i.lemma.id IN (:lemmasIds) AND i.page.id = (:pageId)")
    Integer findPageIdsByLemmaIdsAndPageId(@Param("lemmasIds") List<Integer> lemmasIds, @Param("pageId") Integer pageId);
}
