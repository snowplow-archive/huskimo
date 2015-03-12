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
-- Version:     0.1.0
-- URL:         -
--
-- Authors:     Alex Dean
-- Copyright:   Copyright (c) 2015 Snowplow Analytics Ltd
-- License:     Apache License Version 2.0

 -- Create the schema
CREATE SCHEMA huskimo;

-- Create huskimo user
CREATE USER huskimo PASSWORD 'xxx';

-- Create table for Singular campaigns
CREATE TABLE huskimo.singular_campaigns (
  channel_name        varchar(512) not null encode runlength,
  when_retrieved      timestamp not null encode runlength,
  ad_network          varchar(512) not null encode text32k,
  campaign_name       varchar(512) not null encode text32k,
  campaign_type       varchar(512) not null encode text32k,
  campaign_url        varchar(8192),
  app_id              varchar(512) encode runlength,
  campaign_network_id varchar(512) encode runlength,
  country             varchar(2) encode text255,
  reporting_date      timestamp not null encode runlength, -- Renamed from date to standardize
  impressions         bigint,
  clicks              bigint,
  installs            bigint,
  cost                decimal(10, 2),
  revenue             decimal(10, 2),
  last_modified       timestamp not null
)
DISTSTYLE KEY
DISTKEY (ad_network)
SORTKEY (reporting_date, when_retrieved);

-- Create table for Singular creatives
CREATE TABLE huskimo.singular_creatives (
  channel_name        varchar(512) not null encode runlength,
  when_retrieved      timestamp not null encode runlength,
  ad_network          varchar(512) not null encode text32k,
  creative_name       varchar(512) not null encode text32k,
  creative_text       varchar(512) encode text32k,
  image               varchar(8192) not null encode text32k,
  image_hash          varchar(512) not null encode text32k,
  width               varchar(512) encode text32k,
  height              varchar(512) encode text32k,
  is_video            boolean not null encode runlength,
  campaign_name       varchar(512) not null encode text32k,
  campaign_type       varchar(512) not null encode text32k,
  campaign_url        varchar(8192),
  app_id              varchar(512) encode runlength,
  campaign_network_id varchar(512) encode runlength,
  reporting_date      timestamp not null encode runlength, -- Renamed from date to standardize
  impressions         bigint,
  clicks              bigint,
  installs            bigint,
  cost                decimal(10, 2),
  modified_at         timestamp not null
)
DISTSTYLE KEY
DISTKEY (ad_network)
SORTKEY (reporting_date, when_retrieved);

-- Set permissions
GRANT USAGE ON SCHEMA huskimo TO huskimo;
GRANT INSERT ON TABLE "huskimo"."singular_campaigns" TO huskimo;
GRANT INSERT ON TABLE "huskimo"."singular_creatives" TO huskimo;
