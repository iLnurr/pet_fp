object quiz {
  val html: String =
    """
<!DOCTYPE html>
<html>
<head>
    <title>Product feedback survey example, Vue Survey Library Example</title>

    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="https://unpkg.com/jquery"></script>
    <script src="https://surveyjs.azureedge.net/1.1.21/survey.jquery.js"></script>
    <link href="https://surveyjs.azureedge.net/1.1.21/modern.css" type="text/css" rel="stylesheet"/>
    <link rel="stylesheet" href="./index.css">

</head>
<body>
<div id="surveyElement">
    <survey :survey='survey'/>
</div>

<script>
    $(function () {
        Survey
            .StylesManager
            .applyTheme("modern");

        var json = {
            pages: [
                {
                    name: "stage",
                    elements: [
                        {
                            type: "radiogroup",
                            name: "stage1",
                            title: "Какую недвижимость  вы хотите приобрести?",
                            isRequired: true,
                            choices: [
                                {
                                    value: "construction",
                                    text: "на стадии строительства"
                                },
                                {
                                    value: "new",
                                    text: "новое готовое от застройщика"
                                },
                                {
                                    value: "second",
                                    text: "вторичное жилье"
                                },
                                {
                                    value: "any",
                                    text: "любое"
                                }
                            ],
                            colCount: 2
                        }
                    ]
                },
                {
                    name: "page2",
                    elements: [
                        {
                            type: "radiogroup",
                            name: "type",
                            title: "Укажите тип недвижимости",
                            isRequired: true,
                            choices: [
                                {
                                    value: "item1",
                                    text: "квартира"
                                },
                                {
                                    value: "item2",
                                    text: "дом"
                                },
                                {
                                    value: "item3",
                                    text: "вилла"
                                },
                                {
                                    value: "item4",
                                    text: "отель"
                                },
                                {
                                    value: "item5",
                                    text: "участок"
                                }
                            ],
                            colCount: 2
                        }
                    ]
                },
                {
                    name: "page1",
                    elements: [
                        {
                            type: "multipletext",
                            name: "Заполните форму и мы свяжемся с Вами",
                            isRequired: true,
                            items: [
                                {
                                    name: "name",
                                    placeHolder: "Введите имя"
                                },
                                {
                                    name: "phone",
                                    placeHolder: "Введите номер телефона",
                                    inputType: "tel",
                                    validators: [
                                        {
                                            type: "numeric"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ],
            showProgressBar: "bottom",
            goNextPageAutomatic: true
        };

        window.survey = new Survey.Model(json);


        $("#surveyElement").Survey({model: survey});
    });
</script>

</body>
</html>
      """
}
