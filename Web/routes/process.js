/**
 * Created by yizhazha on 11/20/2017.
 */
var express = require('express');
var path = require('path');
var router = express.Router();


//var oracledb = require('oracledb');
//var dbConfig = require('./dbconfig.js');


var jenkinsapi = require('jenkins-api');


router.get('/', function(req, res) {

    var Product = req.query.Product;
    var UOW = req.query.UOW;
    var URL = req.query.Database;
    var Email = req.query.Email;

    var tempURL = URL.split("/");
    var Server_Port = tempURL[2];

    if(tempURL[3] =="psp")
        var DBName = tempURL[4].slice(0,-1);
    else
        var DBName = tempURL[3].slice(0,-1);
    //console.log(DBName);

    var jenkins = jenkinsapi.init("http://localhost:8080");

    jenkins.build_with_params('Init_Job_Params', {Product: Product, UOW: UOW, URL: URL, DBName: DBName, Server_Port: Server_Port, Email: Email }, function(err, data) {
        if (err){ return console.log(err); }
        console.log(data)
    });
    //console.log(response);
    //res.end(JSON.stringify(response));
    //res.location('process.html');


    /*
     oracledb.getConnection(
     {
     user          : dbConfig.user,
     password      : dbConfig.password,
     connectString : dbConfig.connectString
     },
     function(err, connection)
     {
     if (err) {
     console.error(err.message);
     return;
     }
     connection.execute(
     // The statement to execute
     "SELECT *" +
     "FROM PS_UOW_FUNCPREREQ " +
     "WHERE UOW_ID = :id",

     // The "bind value" 180 for the "bind variable" :id
     [req.query.UOW],

     // Optional execute options argument, such as the query result format
     // or whether to get extra metadata
     { outFormat: oracledb.OBJECT, extendedMetaData: true },

     // The callback function handles the SQL execution results
     function(err, result)
     {
     if (err) {
     console.error(err.message);
     doRelease(connection);
     return;
     }
     //console.log(result.metaData); // [ { name: 'DEPARTMENT_ID' }, { name: 'DEPARTMENT_NAME' } ]
     console.log(result.rows);     // [ [ 180, 'Construction' ] ]

     //res.end(JSON.stringify(result.rows));
     doRelease(connection);
     });




     });

     // Note: connections should always be released when not needed
     function doRelease(connection)
     {
     connection.close(
     function(err) {
     if (err) {
     console.error(err.message);
     }
     });
     }

     */

    res.render('process', {Email : Email});
    //res.sendFile(path.resolve(__dirname, '..') + '/public/process.html');
});

module.exports = router;