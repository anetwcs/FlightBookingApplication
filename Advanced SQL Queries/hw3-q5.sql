/*

Output contains 4 rows
Query took 43s to run
The first 20 rows of result:

Devils Lake ND
Hattiesburg/Laurel MS
Seattle WA
St. Augustine FL

*/




SELECT DISTINCT f.dest_city as city FROM FLIGHTS f WHERE f.dest_city NOT IN (
	SELECT DISTINCT f2.dest_city as city FROM FLIGHTS as f1, FLIGHTS as f2 
WHERE f1.origin_city = 'Seattle WA' AND f1.dest_city = f2.origin_city AND f2.dest_city != 'Seattle WA')
AND f.dest_city NOT IN ( SELECT f3.dest_city FROM FLIGHTS f3 WHERE f3.origin_city = 'Seattle WA')
ORDER BY city ASC;