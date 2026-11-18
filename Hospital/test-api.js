
const http = require('http');

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/hospitals',
  method: 'GET'
};

const req = http.request(options, (res) => {
  console.log(`状态码: ${res.statusCode}`);
  
  let data = '';
  
  res.on('data', (chunk) => {
    data += chunk;
  });
  
  res.on('end', () => {
    console.log('医院列表:', data);
    
    // 测试获取西安交大一附院的医生
    const doctorOptions = {
      hostname: 'localhost',
      port: 8080,
      path: '/api/doctors/hospital/H002',
      method: 'GET'
    };
    
    const doctorReq = http.request(doctorOptions, (docRes) => {
      console.log(`\n医生列表状态码: ${docRes.statusCode}`);
      let docData = '';
      docRes.on('data', (chunk) => { docData += chunk; });
      docRes.on('end', () => {
        console.log('西安交大一附院医生:', docData);
      });
    });
    doctorReq.end();
  });
});

req.on('error', (error) => {
  console.error('请求错误:', error);
  console.log('请确保Java服务正在运行！');
});

req.end();

