/*
Output contains 4 rows
Query took 16s
The first 20 rows of output: 
Alaska Airlines Inc.
SkyWest Airlines Inc.
United Air Lines Inc.
Virgin America
*/



SELECT DISTINCT c.name as name 
FROM CARRIERS AS c, FLIGHTS AS f
WHERE f.origin_city = 'Seattle WA' 
AND f.dest_city = 'San Francisco CA' 
AND c.cid = f.carrier_id;