package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ReportEntity> findFiltered(ReportStatus status,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           Double minLat,
                                           Double maxLat,
                                           Double minLng,
                                           Double maxLng) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReportEntity> query = cb.createQuery(ReportEntity.class);
        Root<ReportEntity> root = query.from(ReportEntity.class);
       // root.fetch("images", JoinType.LEFT);
        query.distinct(true);

        List<Predicate> predicates = new ArrayList<>();
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }
        if (minLat != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), minLat));
        }
        if (maxLat != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), maxLat));
        }
        if (minLng != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), minLng));
        }
        if (maxLng != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), maxLng));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdAt")));

        return entityManager.createQuery(query).setMaxResults(100).getResultList();
    }
}
