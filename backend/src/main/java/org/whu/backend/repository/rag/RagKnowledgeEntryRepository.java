package org.whu.backend.repository.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.rag.RagKnowledgeEntry;

@Repository
public interface RagKnowledgeEntryRepository extends JpaRepository<RagKnowledgeEntry, String> {
}