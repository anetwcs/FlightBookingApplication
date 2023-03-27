--Output contains 334 rows
--The query took 8s to run
/*First 20 rows of result
Aberdeen SD	Minneapolis MN			106
Abilene TX	Dallas/Fort Worth TX		111
Adak Island AK	Anchorage AK			471
Aguadilla PR	New York NY			368
Akron OH	Atlanta GA			408
Albany GA	Atlanta GA			243
Albany NY	Atlanta GA			390
Albuquerque NM	Houston TX			492
Alexandria LA	Atlanta GA			391
Allentown/Bethlehem/Easton PA	Atlanta GA	456
Alpena MI	Detroit MI			80
Amarillo TX	Houston TX			390
Anchorage AK	Barrow AK			490
Appleton WI	Atlanta GA			405
Arcata/Eureka CA	San Francisco CA	476
Asheville NC	Chicago IL			279
Ashland WV	Cincinnati OH			84
Aspen CO	Los Angeles CA			304
Atlanta GA	Honolulu HI			649
Atlantic City NJ	Fort Lauderdale FL	212



*/


SELECT DISTINCT f.origin_city as origin_city, f.dest_city, f.actual_time as time
FROM FLIGHTS as f, (SELECT f1.origin_city, MAX(f1.actual_time) as maxtime FROM FLIGHTS as f1 GROUP BY f1.origin_city ) as fd 
WHERE f.origin_city = fd.origin_city AND f.actual_time = fd.maxtime
ORDER BY f.origin_city, f.dest_city ASC;