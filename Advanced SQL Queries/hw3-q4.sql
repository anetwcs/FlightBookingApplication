
/*
Output contains 256 rows
Query took 12s to run
The first 20 rows of result:

Aberdeen SD
Abilene TX
Adak Island AK
Aguadilla PR
Akron OH
Albany GA
Albany NY
Alexandria LA
Allentown/Bethlehem/Easton PA
Alpena MI
Amarillo TX
Appleton WI
Arcata/Eureka CA
Asheville NC
Ashland WV
Aspen CO
Atlantic City NJ
Augusta GA
Bakersfield CA
Bangor ME
*/

SELECT DISTINCT f2.dest_city as city FROM FLIGHTS as f1, FLIGHTS as f2 
WHERE f1.origin_city = 'Seattle WA' AND f1.dest_city = f2.origin_city AND f2.dest_city != 'Seattle WA'
AND f2.dest_city NOT IN (SELECT f3.dest_city FROM FLIGHTS f3 WHERE f3.origin_city = 'Seattle WA') 
ORDER BY f2.dest_city ASC;   



