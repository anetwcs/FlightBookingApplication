--Output contains 109 row 
--Query took 26s
/*First 20 rows of output:

Aberdeen SD
Abilene TX
Alpena MI
Ashland WV
Augusta GA
Barrow AK
Beaumont/Port Arthur TX
Bemidji MN
Bethel AK
Binghamton NY
Brainerd MN
Bristol/Johnson City/Kingsport TN
Butte MT
Carlsbad CA
Casper WY
Cedar City UT
Chico CA
College Station/Bryan TX
Columbia MO
Columbus GA
*/

SELECT DISTINCT f.origin_city as city 
FROM FLIGHTS as f 
WHERE f.origin_city 
NOT IN (SELECT f1.origin_city FROM FLIGHTS as f1 
WHERE f1.actual_time >= 180 AND f1.canceled = 0)
ORDER BY f.origin_city ASC;