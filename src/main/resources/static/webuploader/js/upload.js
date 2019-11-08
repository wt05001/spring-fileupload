var prefix_url = 'http://localhost:8080/api/upload';
var shardSize = 1 * 1024 * 1024;         //以1MB为一个分片
var GUID = WebUploader.Base.guid();//一个GUID

$('#upload-container').click(function(event) {
    $("#picker").find('input').click();
});

var uploader = WebUploader.create({
    auto: false,// 选完文件后，是否自动上传。
    // swf文件路径
    swf: 'https://cdn.bootcss.com/webuploader/0.1.1/Uploader.swf',
    // 文件接收服务端。
    server: prefix_url + '/files/part',
    formData: {
        guid: GUID
    },
    pick: '#picker',
    chunked: true, // 分片处理
    chunkSize: shardSize, // 每片1M,
    chunkRetry: false,// 如果失败，则不重试
    threads: 1,// 上传并发数。允许同时最大上传进程数。
    resize: false
});

// 文件上传过程中创建进度条实时显示。
uploader.on('uploadProgress', function (file, percentage) {
    var widthTemp = percentage * 100 + '%';
    $('#process').css('width', widthTemp);
    $('#process').text(widthTemp);
});

$(function () {
    $("#startBtn").click(function () {
        uploader.upload();
    });
});

//当文件上传成功时触发。
uploader.on("uploadSuccess", function (file) {
    $.post(prefix_url + "/files/merge", {guid: GUID, fileName: file.name}, function (data) {
        if (data.code == 200) {
            alert('上传成功!');
        }
    });
});