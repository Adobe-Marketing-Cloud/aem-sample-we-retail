/*jslint browser */
/*global window, $, Granite */
(function ($document, author) {
    "use strict";

    const componentPath = "weretail/components";
    const fragmentpath = "/content/contentfragment";
    const fragmentListPath = "/contentfragmentlist/v1/contentfragmentlist";
    const assetPath = "/assets.html";
    const editorPath = "/editor.html";
    const browserIcon = "browse";
    const descText = "Open content fragment";
    let openContentFragment = {
        icon: browserIcon,
        text: descText,
        handler: function (editable) {
            let hostName = window.location.host;
            let protocol = window.location.protocol;
            let domainName = protocol + "//" + hostName;
            let pagePath = domainName + editable.path;
            let currentContentFragmentUrl = pagePath + ".-1.json";
            $.getJSON(currentContentFragmentUrl, function (result) {
                let fragmentUrl = window.location.protocol + "//" + hostName;
                let contentFragmentListPath = assetPath + result.parentPath;
                let isContentFragmentPath;
                if (editable.type === componentPath + fragmentpath) {
                    isContentFragmentPath = true;
                }
                let contentFragmentPath = editorPath + result.fragmentPath;
                let resultFragmentPath;

                if (isContentFragmentPath) {
                    resultFragmentPath = contentFragmentPath;
                } else {
                    resultFragmentPath = contentFragmentListPath;
                }
                if (result.fragmentPath || result.parentPath) {
                    fragmentUrl = fragmentUrl + resultFragmentPath;
                    window.open(fragmentUrl, "_blank");
                }
            });
        },
        condition: function (editable) {
            let editType = editable.type;
            let isContentFragment = (editType === componentPath + fragmentpath);
            let isContentFragmentList;
            if (editType === componentPath + fragmentListPath) {
                isContentFragmentList = true;
            }
            return isContentFragment || isContentFragmentList;
        },
        isNonMulti: true
    };

    $document.on("cq-layer-activated", function (ev) {
        if (ev.layer === "Edit") {
            let editToolbar = author.EditorFrame.editableToolbar;
            editToolbar.registerAction("EAEM_OPEN_DIALOG", openContentFragment);
        }
    });

    function checkContainer() {
        let editToolbar = author.EditorFrame.editableToolbar;
        if ($("#EditableToolbar").length && editToolbar) {
            editToolbar.registerAction("EAEM_OPEN_DIALOG", openContentFragment);
        } else {
            setTimeout(checkContainer, 50); //wait 50 ms, then try again
        }
    }

    //This logic is workaround when 'cq-layer-activated' is not triggered
    $(document).ready(checkContainer);

}($(document), Granite.author));