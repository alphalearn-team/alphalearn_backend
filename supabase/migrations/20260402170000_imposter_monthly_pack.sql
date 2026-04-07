create table if not exists imposter_monthly_packs (
    id bigserial primary key,
    public_id uuid not null default gen_random_uuid(),
    year_month varchar(7) not null,
    created_at timestamptz not null default now(),
    constraint uk_imposter_monthly_packs_public_id unique (public_id),
    constraint uk_imposter_monthly_packs_year_month unique (year_month),
    constraint ck_imposter_monthly_packs_year_month_format check (year_month ~ '^[0-9]{4}-[0-9]{2}$')
);
create table if not exists imposter_monthly_pack_concepts (
    id bigserial primary key,
    pack_id bigint not null,
    concept_id integer not null,
    slot_index smallint not null,
    constraint fk_imposter_pack_concepts_pack
        foreign key (pack_id) references imposter_monthly_packs(id) on delete cascade,
    constraint fk_imposter_pack_concepts_concept
        foreign key (concept_id) references concepts(concept_id),
    constraint uk_imposter_pack_concepts_pack_slot unique (pack_id, slot_index),
    constraint uk_imposter_pack_concepts_pack_concept unique (pack_id, concept_id),
    constraint ck_imposter_pack_concepts_slot_index check (slot_index between 1 and 20)
);
create table if not exists imposter_monthly_pack_weekly_features (
    id bigserial primary key,
    pack_id bigint not null,
    concept_id integer not null,
    week_slot smallint not null,
    constraint fk_imposter_pack_weekly_features_pack
        foreign key (pack_id) references imposter_monthly_packs(id) on delete cascade,
    constraint fk_imposter_pack_weekly_features_concept
        foreign key (concept_id) references concepts(concept_id),
    constraint fk_imposter_pack_weekly_features_pack_concept
        foreign key (pack_id, concept_id) references imposter_monthly_pack_concepts(pack_id, concept_id),
    constraint uk_imposter_pack_weekly_features_pack_week unique (pack_id, week_slot),
    constraint uk_imposter_pack_weekly_features_pack_concept unique (pack_id, concept_id),
    constraint ck_imposter_pack_weekly_features_week_slot check (week_slot between 1 and 4)
);
