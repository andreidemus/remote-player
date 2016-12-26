var fileIcon = "<span class='glyphicon glyphicon-file'></span>";
var dirIcon = "<span class='glyphicon glyphicon-folder-close'></span>";
var parentIcon = "<span class='glyphicon glyphicon-folder-open'></span>";

function cmd(c) {
    console.log("command: " + c);
    $.ajax({
        dataType: "json",
        url: "/" + c
    });
}

function loadMovie(id) {
    cmd("play/" + id)
}

function movieOnClick(event) {
    var item = $(event.target);
    var id = item.data("id");
    var isDir = item.data("is-directory");
    if (isDir) loadPlaylist(id)
    else loadMovie(id)
}

function loadPlaylist(dirId) {
    var url = dirId ? "/playlist/" + dirId : "/playlist"
    console.log(url);
    $.getJSON(url, function (data) {
        var items = [];
        $.each(data, function (key, val) {
            var id = val.id
            var name = val.name;
            var isDirectory = val["is-directory"];
            var isParent = val["is-parent"];
            var icon = "";
            if (isParent) icon = parentIcon;
            else if (isDirectory) icon = dirIcon;
            else icon = fileIcon;
            var item = "<li class='list-group-item movie' id='movie-" + id + "'>"
                + icon
                + " &nbsp;<a href='#' data-id='" + id + "' data-is-directory='" + isDirectory + "'>"
                + name + "</a></li>";
            items.push(item);
        });

        var ul = $("<ul/>", {
            "id": "movies",
            "class": "list-group",
            html: items.join("")
        });
        // $(".movie").off("click", "a", movieOnClick);
        $("ul#movies").replaceWith(ul);
        $(".movie").on("click", "a", movieOnClick);
    });
}

$(document).ready(function () {
        $(".btn-group").on("click", "#play", function () {
            cmd("play-all")
        });
        $(".btn-group").on("click", "#pause", function () {
            cmd("pause")
        });
        $(".btn-group").on("click", "#stop", function () {
            cmd("stop")
        });
        $(".btn-group").on("click", "#mute", function () {
            cmd("mute")
        });
        $(".btn-group").on("click", "#volume-down", function () {
            cmd("volume-down")
        });
        $(".btn-group").on("click", "#volume-up", function () {
            cmd("volume-up")
        });
        $(".btn-group").on("click", "#forward", function () {
            cmd("forward")
        });
        $(".btn-group").on("click", "#f-forward", function () {
            cmd("f-forward")
        });
        $(".btn-group").on("click", "#backward", function () {
            cmd("backward")
        });
        $(".btn-group").on("click", "#f-backward", function () {
            cmd("f-backward")
        });
        $(".btn-group").on("click", "#refresh-playlist", function () {
            loadPlaylist()
        });
        $(".btn-group").on("click", "#switch-subs", function () {
            cmd("switch-subs")
        });

        loadPlaylist()
    }
);
