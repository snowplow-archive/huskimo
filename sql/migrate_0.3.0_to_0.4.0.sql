-- Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
--
-- This program is licensed to you under the Apache License Version 2.0,
-- and you may not use this file except in compliance with the Apache License Version 2.0.
-- You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the Apache License Version 2.0 is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
--
-- Version:     0.3.0 to 0.4.0
-- URL:         -
--
-- Copyright:   Copyright (c) 2015 Snowplow Analytics Ltd
-- License:     Apache License Version 2.0

-- First rename the existing table (don't delete it)
ALTER TABLE huskimo.singular_campaigns RENAME TO singular_campaigns_030;
ALTER TABLE huskimo.singular_creatives RENAME TO singular_creatives_030;

-- Create table for Singular campaigns
CREATE TABLE huskimo.singular_campaigns (
  channel_name        VARCHAR(512) NOT NULL ENCODE RUNLENGTH,
  when_retrieved      TIMESTAMP    NOT NULL ENCODE LZO,
  ad_network          VARCHAR(512) NOT NULL ENCODE LZO,
  campaign_name       VARCHAR(512) NOT NULL ENCODE LZO,
  campaign_type       VARCHAR(512) NOT NULL ENCODE LZO,
  campaign_url        VARCHAR(8192)         ENCODE LZO,
  subcampaign_name    VARCHAR(512)          ENCODE LZO,
  app_id              VARCHAR(512)          ENCODE LZO,
  campaign_network_id VARCHAR(512)          ENCODE LZO, -- Always empty
  country             VARCHAR(2)            ENCODE LZO,
  reporting_date      TIMESTAMP NOT NULL    ENCODE LZO, -- Renamed from date$
  impressions         BIGINT                ENCODE LZO,
  clicks              BIGINT                ENCODE LZO,
  installs            BIGINT                ENCODE LZO,
  cost                DECIMAL(10, 2)        ENCODE LZO,
  revenue             DECIMAL(10, 2)        ENCODE LZO,
  last_modified       TIMESTAMP NOT NULL    ENCODE LZO
)
DISTSTYLE KEY
DISTKEY (ad_network)
SORTKEY (reporting_date, when_retrieved);

-- Create table for Singular creatives
CREATE TABLE huskimo.singular_creatives (
  channel_name        VARCHAR(512)  NOT NULL ENCODE RUNLENGTH,
  when_retrieved      TIMESTAMP     NOT NULL ENCODE LZO,
  ad_network          VARCHAR(512)  NOT NULL ENCODE LZO,
  creative_name       VARCHAR(512)           ENCODE LZO,
  creative_text       VARCHAR(512)           ENCODE LZO,
  creative_url        VARCHAR(8192)          ENCODE LZO,
  creative_network_id VARCHAR(512)           ENCODE LZO,
  image               VARCHAR(8192) NOT NULL ENCODE LZO,
  image_hash          VARCHAR(512)  NOT NULL ENCODE LZO,
  width               INTEGER                ENCODE LZO,
  height              INTEGER                ENCODE LZO,
  is_video            BOOLEAN       NOT NULL ENCODE RAW,
  campaign_name       VARCHAR(512)  NOT NULL ENCODE LZO,
  campaign_type       VARCHAR(512)  NOT NULL ENCODE LZO,
  campaign_url        VARCHAR(8192)          ENCODE LZO,
  subcampaign_name    VARCHAR(512)           ENCODE LZO,
  app_id              VARCHAR(512)           ENCODE LZO,
  campaign_network_id VARCHAR(512)           ENCODE LZO,
  reporting_date      TIMESTAMP     NOT NULL ENCODE LZO, -- Renamed from date$
  impressions         BIGINT                 ENCODE LZO,
  clicks              BIGINT                 ENCODE LZO,
  installs            BIGINT                 ENCODE LZO,
  cost                DECIMAL(10, 2)         ENCODE LZO,
  modified_at         TIMESTAMP NOT NULL     ENCODE LZO
)
DISTSTYLE KEY
DISTKEY (ad_network)
SORTKEY (reporting_date, when_retrieved);

INSERT INTO huskimo.singular_campaigns
  SELECT * FROM huskimo.singular_campaigns_030;

INSERT INTO huskimo.singular_creatives
  SELECT channel_name,
         when_retrieved,
         ad_network,
         creative_name,
         creative_text,
         NULL,  -- Added in 0.4.0
         NULL,  -- Added in 0.4.0
         image,
         image_hash,
         CAST(width AS INTEGER),  -- Modified in 0.4.0, VARCHAR -> INTEGER
         CAST(height AS INTEGER), -- Modified in 0.4.0, VARCHAR -> INTEGER
         is_video,
         campaign_name,
         campaign_type,
         campaign_url,
         subcampaign_name,
         app_id,
         campaign_network_id,
         reporting_date,
         impressions,
         clicks,
         installs,
         cost,
         modified_at
    FROM huskimo.singular_creatives_030;

-- Set permissions
GRANT USAGE ON SCHEMA huskimo TO huskimo;
GRANT INSERT ON TABLE "huskimo"."singular_campaigns" TO huskimo;
GRANT INSERT ON TABLE "huskimo"."singular_creatives" TO huskimo;
