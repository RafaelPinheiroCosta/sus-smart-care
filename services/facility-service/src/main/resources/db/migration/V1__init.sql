CREATE TABLE health_facilities (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE care_zones (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_care_zone_facility
        FOREIGN KEY (facility_id)
        REFERENCES health_facilities(id),

    CONSTRAINT uq_care_zone_facility_code
        UNIQUE (facility_id, code)
);

CREATE INDEX idx_care_zones_facility
    ON care_zones(facility_id);

CREATE TABLE beds (
    id UUID PRIMARY KEY,
    zone_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    type VARCHAR(30) NOT NULL,
    operational_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_bed_zone
        FOREIGN KEY (zone_id)
        REFERENCES care_zones(id),

    CONSTRAINT uq_bed_zone_code
        UNIQUE (zone_id, code)
);

CREATE INDEX idx_beds_zone
    ON beds(zone_id);

CREATE TABLE bed_occupations (
    id UUID PRIMARY KEY,
    bed_id UUID NOT NULL,
    visit_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,

    CONSTRAINT fk_bed_occupation_bed
        FOREIGN KEY (bed_id)
        REFERENCES beds(id)
);

CREATE INDEX idx_bed_occupations_bed
    ON bed_occupations(bed_id);

CREATE INDEX idx_bed_occupations_visit
    ON bed_occupations(visit_id);

CREATE UNIQUE INDEX uq_active_occupation_bed
    ON bed_occupations(bed_id)
    WHERE ended_at IS NULL;

CREATE UNIQUE INDEX uq_active_occupation_visit
    ON bed_occupations(visit_id)
    WHERE ended_at IS NULL;
