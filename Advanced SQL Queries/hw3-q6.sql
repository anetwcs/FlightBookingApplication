/*
Output contains 4 rows
Query took 15s
The first 20 rows of output: 
Alaska Airlines Inc.
SkyWest Airlines Inc.
United Air Lines Inc.
Virgin America
*/



SELECT DISTINCT c.name as name FROM CARRIERS AS c, (
		SELECT f.carrier_id as carrier_id FROM FLIGHTS AS f
		WHERE f.origin_city = 'Seattle WA' AND f.dest_city = 'San Francisco CA') as ss
		WHERE c.cid = ss.carrier_id;