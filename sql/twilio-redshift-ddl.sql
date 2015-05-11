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

-- Create table for Twilio calls
CREATE TABLE huskimo.twilio_calls (
  channel_name         varchar(512) not null encode runlength ,
  when_retrieved       timestamp    not null encode runlength ,
  sid                  varchar(34)  not null encode raw       ,
  account_sid          varchar(34)  not null encode runlength ,
  api_version          varchar(128) not null  encode text255  ,
  date_created         timestamp    not null                  ,
  date_updated         timestamp    not null                  ,
  parent_call_sid      varchar(34)                            ,
  "to"                 varchar(512)          encode raw       ,
  "from"               varchar(512)          encode raw       ,
  phone_number_sid     varchar(34)                            ,
  status               varchar(11)           encode bytedict  ,
  start_time           timestamp    not null                  ,
  end_time             timestamp                              ,
  duration             varchar(512)          encode raw       , -- todo: why a string?
  price                varchar(512)          encode raw       , -- todo: why a string?
  direction            varchar(512)          encode raw       ,
  answered_by          varchar(512)          encode runlength ,
  forwarded_from       varchar(512)          encode runlength ,
  caller_name          varchar(512)          encode raw
)
DISTSTYLE KEY
DISTKEY (account_sid)
SORTKEY (date_created, when_retrieved);

-- Create table for Twilio incoming phone numbers
CREATE TABLE huskimo.twilio_incoming_phone_numbers (
  channel_name           varchar(512) not null encode runlength ,
  when_retrieved         timestamp    not null encode runlength ,
  sid                    varchar(34)  not null encode text32k   ,
  account_sid            varchar(34)  not null encode runlength ,
  api_version            varchar(128) not null encode text255   ,
  date_created           timestamp    not null                  ,
  date_updated           timestamp    not null                  ,
  friendly_name          varchar(64)           encode raw       ,
  phone_number           varchar(128)          encode raw       ,
  voice_application_sid  varchar(128)          encode raw       ,
  sms_application_sid    varchar(128)          encode raw       ,
  voice_url              varchar(8012)         encode raw       ,
  voice_method           varchar(4)            encode bytedict  ,
  voice_fallback_url     varchar(8012)         encode raw       ,
  voice_fallback_method  varchar(4)            encode bytedict  ,
  status_callback        varchar(8012)         encode raw       ,
  status_fallback_method varchar(4)            encode bytedict  ,
  voice_caller_id_lookup boolean               encode runlength ,
  sms_url                varchar(8012)         encode raw       ,
  sms_method             varchar(512)          encode raw       ,
  sms_fallback_url       varchar(8012)         encode raw       ,
  sms_fallback_method    varchar(4)            encode bytedict  ,
  sms_status_callback    varchar(128)          encode raw       ,
  address_requirements   varchar(12)           encode raw
)
DISTSTYLE KEY
DISTKEY (account_sid)
SORTKEY (date_created, when_retrieved);

-- Create table for Twilio messages
CREATE TABLE huskimo.twilio_messages (
  channel_name         varchar(512) not null encode runlength ,
  when_retrieved       timestamp    not null encode runlength ,
  sid                  varchar(34)  not null encode raw       ,
  account_sid          varchar(34)  not null encode runlength ,
  api_version          varchar(128) not null encode text255   ,
  date_created         timestamp    not null                  ,
  date_updated         timestamp    not null                  ,
  date_sent            timestamp    not null                  ,
  "to"                 varchar(512)          encode raw       ,
  "from"               varchar(512)          encode raw       ,
  body_length          bigint                encode raw       , -- Obscuring body for privacy reasons
  status               varchar(12)           encode text255   ,
  price                varchar(128)          encode runlength ,
  price_unit           varchar(3)            encode text255   ,
  num_media            bigint                encode runlength ,
  num_segments         bigint                encode runlength ,
  direction            varchar(14)           encode bytedict  ,
  error_message        varchar(512)          encode text32k
)
DISTSTYLE KEY
DISTKEY (account_sid)
SORTKEY (date_created, when_retrieved);

-- Create table for Twilio recordings
CREATE TABLE huskimo.twilio_recordings (
  channel_name         varchar(512) not null encode runlength ,
  when_retrieved       timestamp    not null encode runlength ,
  sid                  varchar(34)  not null encode raw       ,
  account_sid          varchar(34)  not null encode runlength ,
  api_version          varchar(128) not null encode text255   ,
  date_created         timestamp    not null                  ,
  date_updated         timestamp    not null                  ,
  call_sid             varchar(34)           encode text32k   ,
  duration             bigint                encode runlength
)
DISTSTYLE KEY
DISTKEY (account_sid)
SORTKEY (date_created, when_retrieved);

-- Create table for the Twilio pricing phone numbers
CREATE TABLE huskimo.twilio_pricing_phone_numbers (
  channel_name         varchar(512) not null encode runlength ,
  when_retrieved       timestamp    not null encode runlength ,
  country              varchar(34)  not null encode runlength ,
  iso_country          char(2)      not null encode runlength ,
  price_unit           char(3)      not null encode runlength ,
  number_type          varchar(9)   not null encode text255   ,
  base_price           varchar(128) not null encode text32k   ,
  current_price        varchar(128) not null encode text32k
)
DISTSTYLE KEY
DISTKEY (iso_country)
SORTKEY (when_retrieved);

-- Set permissions
GRANT INSERT, SELECT ON TABLE "huskimo"."twilio_calls" TO huskimo;
GRANT INSERT, SELECT, DELETE ON TABLE "huskimo"."twilio_incoming_phone_numbers" TO huskimo;
GRANT INSERT, SELECT ON TABLE "huskimo"."twilio_messages" TO huskimo;
GRANT INSERT, SELECT ON TABLE "huskimo"."twilio_recordings" TO huskimo;
GRANT INSERT, SELECT, DELETE ON TABLE "huskimo"."twilio_pricing_phone_numbers" TO huskimo;
