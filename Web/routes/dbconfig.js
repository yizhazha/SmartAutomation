/**
 * Created by yizhazha on 10/31/2017.
 */
module.exports = {
    user          : process.env.NODE_ORACLEDB_USER || "people",
    password      : process.env.NODE_ORACLEDB_PASSWORD || "peop1e",
    connectString : process.env.NODE_ORACLEDB_CONNECTIONSTRING || "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=slcg61-scan1)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=EMHD88PR.us.oracle.com)))"
};