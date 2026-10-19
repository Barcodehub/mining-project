package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
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
    private EntityManager em;

    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            Root<EnvironmentalDataEntity> root,
                                            EnvironmentalDataFilter f) {
        List<Predicate> p = new ArrayList<>();

        if (f.getStartDate() != null)         p.add(cb.greaterThanOrEqualTo(root.get("timestamp"), f.getStartDate()));
        if (f.getEndDate() != null)           p.add(cb.lessThanOrEqualTo(root.get("timestamp"), f.getEndDate()));
        if (f.getMinLat() != null)            p.add(cb.greaterThanOrEqualTo(root.get("latitude"), f.getMinLat()));
        if (f.getMaxLat() != null)            p.add(cb.lessThanOrEqualTo(root.get("latitude"), f.getMaxLat()));
        if (f.getMinLng() != null)            p.add(cb.greaterThanOrEqualTo(root.get("longitude"), f.getMinLng()));
        if (f.getMaxLng() != null)            p.add(cb.lessThanOrEqualTo(root.get("longitude"), f.getMaxLng()));
        if (f.getPhMin() != null)             p.add(cb.greaterThanOrEqualTo(root.get("ph"), f.getPhMin()));
        if (f.getPhMax() != null)             p.add(cb.lessThanOrEqualTo(root.get("ph"), f.getPhMax()));
        if (f.getTurbidityMin() != null)      p.add(cb.greaterThanOrEqualTo(root.get("turbidity"), f.getTurbidityMin()));
        if (f.getTurbidityMax() != null)      p.add(cb.lessThanOrEqualTo(root.get("turbidity"), f.getTurbidityMax()));
        if (f.getTemperatureMin() != null)    p.add(cb.greaterThanOrEqualTo(root.get("temperature"), f.getTemperatureMin()));
        if (f.getTemperatureMax() != null)    p.add(cb.lessThanOrEqualTo(root.get("temperature"), f.getTemperatureMax()));
        if (f.getRedoxMin() != null)          p.add(cb.greaterThanOrEqualTo(root.get("redox"), f.getRedoxMin()));
        if (f.getRedoxMax() != null)          p.add(cb.lessThanOrEqualTo(root.get("redox"), f.getRedoxMax()));
        if (f.getDissolvedOxygenMin() != null) p.add(cb.greaterThanOrEqualTo(root.get("dissolvedOxygen"), f.getDissolvedOxygenMin()));
        if (f.getOrganicCarbonMin() != null)  p.add(cb.greaterThanOrEqualTo(root.get("organicCarbon"), f.getOrganicCarbonMin()));
        if (f.getHgDisssolvedMin() != null)   p.add(cb.greaterThanOrEqualTo(root.get("hgDissolved"), f.getHgDisssolvedMin()));
        if (f.getHgTotalMin() != null)        p.add(cb.greaterThanOrEqualTo(root.get("hgTotal"), f.getHgTotalMin()));
        if (f.getHgSedimentMin() != null)     p.add(cb.greaterThanOrEqualTo(root.get("hgSediment"), f.getHgSedimentMin()));
        if (f.getHgFishMin() != null)         p.add(cb.greaterThanOrEqualTo(root.get("hgFish"), f.getHgFishMin()));
        if (f.getMethylmercuryMin() != null)  p.add(cb.greaterThanOrEqualTo(root.get("methylmercury"), f.getMethylmercuryMin()));
        if (f.getAlertLevel() != null)        p.add(cb.equal(root.get("alertLevel"), f.getAlertLevel()));
        if (f.getAlertType() != null)         p.add(cb.equal(root.get("alertType"), f.getAlertType()));

        return p;
    }

    @Override
    public List<EnvironmentalDataEntity> findFiltered(EnvironmentalDataFilter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EnvironmentalDataEntity> q = cb.createQuery(EnvironmentalDataEntity.class);
        Root<EnvironmentalDataEntity> root = q.from(EnvironmentalDataEntity.class);
        q.where(buildPredicates(cb, root, filter).toArray(new Predicate[0]));
        q.orderBy(cb.desc(root.get("timestamp")));
        return em.createQuery(q).setMaxResults(10).getResultList();
    }

    @Override
    public EnvironmentalAggregate getAggregated(EnvironmentalDataFilter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<EnvironmentalDataEntity> root = q.from(EnvironmentalDataEntity.class);
        q.where(buildPredicates(cb, root, filter).toArray(new Predicate[0]));

        q.multiselect(
            // pH
            cb.avg(root.get("ph")), cb.min(root.get("ph")), cb.max(root.get("ph")),
            // turbidity
            cb.avg(root.get("turbidity")), cb.min(root.get("turbidity")), cb.max(root.get("turbidity")),
            // temperature
            cb.avg(root.get("temperature")), cb.min(root.get("temperature")), cb.max(root.get("temperature")),
            // redox
            cb.avg(root.get("redox")), cb.min(root.get("redox")), cb.max(root.get("redox")),
            // dissolved oxygen
            cb.avg(root.get("dissolvedOxygen")), cb.min(root.get("dissolvedOxygen")), cb.max(root.get("dissolvedOxygen")),
            // hg dissolved
            cb.avg(root.get("hgDissolved")), cb.min(root.get("hgDissolved")), cb.max(root.get("hgDissolved")),
            // hg total
            cb.avg(root.get("hgTotal")), cb.min(root.get("hgTotal")), cb.max(root.get("hgTotal")),
            // hg sediment
            cb.avg(root.get("hgSediment")), cb.min(root.get("hgSediment")), cb.max(root.get("hgSediment")),
            // hg fish
            cb.avg(root.get("hgFish")), cb.min(root.get("hgFish")), cb.max(root.get("hgFish")),
            // methylmercury
            cb.avg(root.get("methylmercury")), cb.min(root.get("methylmercury")), cb.max(root.get("methylmercury")),
            // scores
            cb.avg(root.get("totalRiskScore")),
            cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("alertLevel"), "CRITICO"), 1L).otherwise(0L)),
            cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("alertLevel"), "ALERTA"), 1L).otherwise(0L))
        );

        Tuple t = em.createQuery(q).getSingleResult();
        int i = 0;
        return new EnvironmentalAggregate(
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class), t.get(i++, Double.class), t.get(i++, Double.class),
            t.get(i++, Double.class),
            t.get(i++, Long.class),
            t.get(i, Long.class)
        );
    }
}
