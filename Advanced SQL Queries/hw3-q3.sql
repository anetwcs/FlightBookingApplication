/*
Output contains 327 rows
Query took 7s to run
The first 20 rows of result:

Guam TT	0
Pago Pago TT	0
Aguadilla PR	28.8973384
Anchorage AK	31.81208054
San Juan PR	33.6605317
Charlotte Amalie VI	39.55882353
Ponce PR	40.98360656
Fairbanks AK	50.11655012
Kahului HI	53.51447135
Honolulu HI	54.73902882
San Francisco CA	55.82886454
Los Angeles CA	56.08089082
Seattle WA	57.60938779
Long Beach CA	62.17643951
New York NY	62.37183414
Kona HI	63.16079295
Las Vegas NV	64.92025637
Christiansted VI	65.10067114
Newark NJ	65.8499711
Plattsburgh NY	66.66666667


*/




SELECT fd.oc2 as origin_city, fn.num*100.0/fd.denom as percentage FROM (
		SELECT  f1.origin_city as oc1, SUM(CASE WHEN f1.actual_time < 180 THEN 1 ELSE 0 END) as num
		FROM FLIGHTS f1 
		WHERE ( f1.canceled = 0)
		GROUP BY f1.origin_city) as fn RIGHT JOIN

		
		(SELECT COUNT(f2.dest_city) as denom, f2.origin_city as oc2 FROM FLIGHTS f2 
		WHERE f2.canceled = 0
		GROUP BY f2.origin_city ) as fd 
		ON fn.oc1 = fd.oc2
		ORDER BY percentage, origin_city ASC;