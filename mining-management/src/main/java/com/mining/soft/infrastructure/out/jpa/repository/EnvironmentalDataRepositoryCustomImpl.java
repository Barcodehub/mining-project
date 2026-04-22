package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class EnvironmentalDataRepositoryCustomImpl implements EnvironmentalDataRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<EnvironmentalDataEntity> root, EnvironmentalDataFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getStartDate() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), filter.getStartDate()));
        if (filter.getEndDate() != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), filter.getEndDate()));
        if (filter.getMinLat() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), filter.getMinLat()));
        if (filter.getMaxLat() != null) predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), filter.getMaxLat()));
        if (filter.getMinLng() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), filter.getMinLng()));
        if (filter.getMaxLng() != null) predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), filter.getMaxLng()));
        if (filter.getPhMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("ph"), filter.getPhMin()));
        if (filter.getPhMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("ph"), filter.getPhMax()));
        if (filter.getTurbidityMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("turbidity"), filter.getTurbidityMin()));
        if (filter.getTurbidityMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("turbidity"), filter.getTurbidityMax()));
        if (filter.getTemperatureMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("temperature"), filter.getTemperatureMin()));
        if (filter.getTemperatureMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("temperature"), filter.getTemperatureMax()));
        if (filter.getMercuryMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("mercury"), filter.getMercuryMin()));
        if (filter.getMercuryMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("mercury"), filter.getMercuryMax()));
        return predicates;
    }

    @Override
    public List<EnvironmentalDataEntity> findFiltered(EnvironmentalDataFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EnvironmentalDataEntity> query = cb.createQuery(EnvironmentalDataEntity.class);
        Root<EnvironmentalDataEntity> root = query.from(EnvironmentalDataEntity.class);

        query.where(buildPredicates(cb, root, filter).toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("timestamp")));
        return entityManager.createQuery(query).setMaxResults(100).getResultList();
    }

    @Override
    public EnvironmentalAggregate getAggregated(EnvironmentalDataFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<EnvironmentalDataEntity> root = query.from(EnvironmentalDataEntity.class);

        query.where(buildPredicates(cb, root, filter).toArray(new Predicate[0]));

        query.multiselect(
                cb.avg(root.get("ph")), cb.min(root.get("ph")), cb.max(root.get("ph")),
                cb.avg(root.get("turbidity")), cb.min(root.get("turbidity")), cb.max(root.get("turbidity")),
                cb.avg(root.get("temperature")), cb.min(root.get("temperature")), cb.max(root.get("temperature")),
                cb.avg(root.get("mercury")), cb.min(root.get("mercury")), cb.max(root.get("mercury"))
        );

        Tuple tuple = entityManager.createQuery(query).getSingleResult();
        return new EnvironmentalAggregate(
                tuple.get(0, Double.class), tuple.get(1, Double.class), tuple.get(2, Double.class),
                tuple.get(3, Double.class), tuple.get(4, Double.class), tuple.get(5, Double.class),
                tuple.get(6, Double.class), tuple.get(7, Double.class), tuple.get(8, Double.class),
                tuple.get(9, Double.class), tuple.get(10, Double.class), tuple.get(11, Double.class)
        );
    }
}
