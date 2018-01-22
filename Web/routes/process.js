/**
 * Created by yizhazha on 11/20/2017.
 */
var express = require('express');
var path = require('path');
var router = express.Router();


//var oracledb = require('oracledb');
//var dbConfig = require('./dbconfig.js');


var jenkinsapi = require('jenkins-api');
var url = require("url");


router.get('/', function(req, res) {

    var Product = req.query.Product;
    var UOW = req.query.UOW;
    var URL = url.parse(req.query.Database);
    console.log(URL);
    var Email = req.query.Email;


    var newURL = URL.href.slice(0,URL.href.indexOf('&'));
    console.log(newURL);
    var DBName = URL.pathname.slice(URL.pathname.indexOf('/psp/')+5,-2).toUpperCase();
    console.log(DBName);
    var serverport = URL.host.replace(8000,8001);
    console.log(serverport);

    //var URLcode = encodeURIComponent(URL);
    //console.log(URLcode);

    var jenkins = jenkinsapi.init("http://den00qhy.us.oracle.com:8090");
    //var jenkins = jenkinsapi.init("http://localhost:8080");

   jenkins.build_with_params('Init_Job_Params', {Product: Product, UOW: UOW, URL: newURL, DBName: DBName, Server_Port: serverport, Email: Email }, function(err, data) {
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