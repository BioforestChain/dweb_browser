package org.dweb_browser.helper

public expect fun String.isRealDomain(): Boolean
public expect fun String.toPunyCode(): String

/**
 * 参考阿里云的js解析代码实现的域名格式解析
 *
 * https://www.sojson.com/blog/312.html
 */
public fun String.parseAsDomain(): DomainParser = DomainParser(this.toPunyCode()).apply { parse() }

public fun String.isMaybeDomain(): Boolean = parseAsDomain().hasError.not()

/**
 * 裁掉url末尾的/后进行比较是否相等
 */
public fun String.isTrimEndSlashEqual(url: String): Boolean = this.trimEnd('/') == url.trimEnd('/')


/**
 * 所有的顶级域名
 * 参考资料
 * https://data.iana.org/TLD/tlds-alpha-by-domain.txt
 *
 * 在控制台中执行代码可以获得：
 * copy(document.body.textContent.toLowerCase().trim().split('\n').filter(it=>!it.startsWith('#')).join(','))
 *
 * 注意，这里转译过了
 */
private val oneTopLevel =
  "aaa,aarp,abb,abbott,abbvie,abc,able,abogado,abudhabi,ac,academy,accenture,accountant,accountants,aco,actor,ad,ads,adult,ae,aeg,aero,aetna,af,afl,africa,ag,agakhan,agency,ai,aig,airbus,airforce,airtel,akdn,al,alibaba,alipay,allfinanz,allstate,ally,alsace,alstom,am,amazon,americanexpress,americanfamily,amex,amfam,amica,amsterdam,analytics,android,anquan,anz,ao,aol,apartments,app,apple,aq,aquarelle,ar,arab,aramco,archi,army,arpa,art,arte,as,asda,asia,associates,at,athleta,attorney,au,auction,audi,audible,audio,auspost,author,auto,autos,avianca,aw,aws,ax,axa,az,azure,ba,baby,baidu,banamex,band,bank,bar,barcelona,barclaycard,barclays,barefoot,bargains,baseball,basketball,bauhaus,bayern,bb,bbc,bbt,bbva,bcg,bcn,bd,be,beats,beauty,beer,bentley,berlin,best,bestbuy,bet,bf,bg,bh,bharti,bi,bible,bid,bike,bing,bingo,bio,biz,bj,black,blackfriday,blockbuster,blog,bloomberg,blue,bm,bms,bmw,bn,bnpparibas,bo,boats,boehringer,bofa,bom,bond,boo,book,booking,bosch,bostik,boston,bot,boutique,box,br,bradesco,bridgestone,broadway,broker,brother,brussels,bs,bt,build,builders,business,buy,buzz,bv,bw,by,bz,bzh,ca,cab,cafe,cal,call,calvinklein,cam,camera,camp,canon,capetown,capital,capitalone,car,caravan,cards,care,career,careers,cars,casa,case,cash,casino,cat,catering,catholic,cba,cbn,cbre,cc,cd,center,ceo,cern,cf,cfa,cfd,cg,ch,chanel,channel,charity,chase,chat,cheap,chintai,christmas,chrome,church,ci,cipriani,circle,cisco,citadel,citi,citic,city,ck,cl,claims,cleaning,click,clinic,clinique,clothing,cloud,club,clubmed,cm,cn,co,coach,codes,coffee,college,cologne,com,commbank,community,company,compare,computer,comsec,condos,construction,consulting,contact,contractors,cooking,cool,coop,corsica,country,coupon,coupons,courses,cpa,cr,credit,creditcard,creditunion,cricket,crown,crs,cruise,cruises,cu,cuisinella,cv,cw,cx,cy,cymru,cyou,cz,dabur,dad,dance,data,date,dating,datsun,day,dclk,dds,de,deal,dealer,deals,degree,delivery,dell,deloitte,delta,democrat,dental,dentist,desi,design,dev,dhl,diamonds,diet,digital,direct,directory,discount,discover,dish,diy,dj,dk,dm,dnp,do,docs,doctor,dog,domains,dot,download,drive,dtv,dubai,dunlop,dupont,durban,dvag,dvr,dz,earth,eat,ec,eco,edeka,edu,education,ee,eg,email,emerck,energy,engineer,engineering,enterprises,epson,equipment,er,ericsson,erni,es,esq,estate,et,eu,eurovision,eus,events,exchange,expert,exposed,express,extraspace,fage,fail,fairwinds,faith,family,fan,fans,farm,farmers,fashion,fast,fedex,feedback,ferrari,ferrero,fi,fidelity,fido,film,final,finance,financial,fire,firestone,firmdale,fish,fishing,fit,fitness,fj,fk,flickr,flights,flir,florist,flowers,fly,fm,fo,foo,food,football,ford,forex,forsale,forum,foundation,fox,fr,free,fresenius,frl,frogans,frontier,ftr,fujitsu,fun,fund,furniture,futbol,fyi,ga,gal,gallery,gallo,gallup,game,games,gap,garden,gay,gb,gbiz,gd,gdn,ge,gea,gent,genting,george,gf,gg,ggee,gh,gi,gift,gifts,gives,giving,gl,glass,gle,global,globo,gm,gmail,gmbh,gmo,gmx,gn,godaddy,gold,goldpoint,golf,goo,goodyear,goog,google,gop,got,gov,gp,gq,gr,grainger,graphics,gratis,green,gripe,grocery,group,gs,gt,gu,gucci,guge,guide,guitars,guru,gw,gy,hair,hamburg,hangout,haus,hbo,hdfc,hdfcbank,health,healthcare,help,helsinki,here,hermes,hiphop,hisamitsu,hitachi,hiv,hk,hkt,hm,hn,hockey,holdings,holiday,homedepot,homegoods,homes,homesense,honda,horse,hospital,host,hosting,hot,hotels,hotmail,house,how,hr,hsbc,ht,hu,hughes,hyatt,hyundai,ibm,icbc,ice,icu,id,ie,ieee,ifm,ikano,il,im,imamat,imdb,immo,immobilien,in,inc,industries,infiniti,info,ing,ink,institute,insurance,insure,int,international,intuit,investments,io,ipiranga,iq,ir,irish,is,ismaili,ist,istanbul,it,itau,itv,jaguar,java,jcb,je,jeep,jetzt,jewelry,jio,jll,jm,jmp,jnj,jo,jobs,joburg,jot,joy,jp,jpmorgan,jprs,juegos,juniper,kaufen,kddi,ke,kerryhotels,kerrylogistics,kerryproperties,kfh,kg,kh,ki,kia,kids,kim,kindle,kitchen,kiwi,km,kn,koeln,komatsu,kosher,kp,kpmg,kpn,kr,krd,kred,kuokgroup,kw,ky,kyoto,kz,la,lacaixa,lamborghini,lamer,lancaster,land,landrover,lanxess,lasalle,lat,latino,latrobe,law,lawyer,lb,lc,lds,lease,leclerc,lefrak,legal,lego,lexus,lgbt,li,lidl,life,lifeinsurance,lifestyle,lighting,like,lilly,limited,limo,lincoln,link,lipsy,live,living,lk,llc,llp,loan,loans,locker,locus,lol,london,lotte,lotto,love,lpl,lplfinancial,lr,ls,lt,ltd,ltda,lu,lundbeck,luxe,luxury,lv,ly,ma,madrid,maif,maison,makeup,man,management,mango,map,market,marketing,markets,marriott,marshalls,mattel,mba,mc,mckinsey,md,me,med,media,meet,melbourne,meme,memorial,men,menu,merckmsd,mg,mh,miami,microsoft,mil,mini,mint,mit,mitsubishi,mk,ml,mlb,mls,mm,mma,mn,mo,mobi,mobile,moda,moe,moi,mom,monash,money,monster,mormon,mortgage,moscow,moto,motorcycles,mov,movie,mp,mq,mr,ms,msd,mt,mtn,mtr,mu,museum,music,mv,mw,mx,my,mz,na,nab,nagoya,name,natura,navy,nba,nc,ne,nec,net,netbank,netflix,network,neustar,new,news,next,nextdirect,nexus,nf,nfl,ng,ngo,nhk,ni,nico,nike,nikon,ninja,nissan,nissay,nl,no,nokia,norton,now,nowruz,nowtv,np,nr,nra,nrw,ntt,nu,nyc,nz,obi,observer,office,okinawa,olayan,olayangroup,ollo,om,omega,one,ong,onl,online,ooo,open,oracle,orange,org,organic,origins,osaka,otsuka,ott,ovh,pa,page,panasonic,paris,pars,partners,parts,party,pay,pccw,pe,pet,pf,pfizer,pg,ph,pharmacy,phd,philips,phone,photo,photography,photos,physio,pics,pictet,pictures,pid,pin,ping,pink,pioneer,pizza,pk,pl,place,play,playstation,plumbing,plus,pm,pn,pnc,pohl,poker,politie,porn,post,pr,pramerica,praxi,press,prime,pro,prod,productions,prof,progressive,promo,properties,property,protection,pru,prudential,ps,pt,pub,pw,pwc,py,qa,qpon,quebec,quest,racing,radio,re,read,realestate,realtor,realty,recipes,red,redstone,redumbrella,rehab,reise,reisen,reit,reliance,ren,rent,rentals,repair,report,republican,rest,restaurant,review,reviews,rexroth,rich,richardli,ricoh,ril,rio,rip,ro,rocks,rodeo,rogers,room,rs,rsvp,ru,rugby,ruhr,run,rw,rwe,ryukyu,sa,saarland,safe,safety,sakura,sale,salon,samsclub,samsung,sandvik,sandvikcoromant,sanofi,sap,sarl,sas,save,saxo,sb,sbi,sbs,sc,scb,schaeffler,schmidt,scholarships,school,schule,schwarz,science,scot,sd,se,search,seat,secure,security,seek,select,sener,services,seven,sew,sex,sexy,sfr,sg,sh,shangrila,sharp,shaw,shell,shia,shiksha,shoes,shop,shopping,shouji,show,si,silk,sina,singles,site,sj,sk,ski,skin,sky,skype,sl,sling,sm,smart,smile,sn,sncf,so,soccer,social,softbank,software,sohu,solar,solutions,song,sony,soy,spa,space,sport,spot,sr,srl,ss,st,stada,staples,star,statebank,statefarm,stc,stcgroup,stockholm,storage,store,stream,studio,study,style,su,sucks,supplies,supply,support,surf,surgery,suzuki,sv,swatch,swiss,sx,sy,sydney,systems,sz,tab,taipei,talk,taobao,target,tatamotors,tatar,tattoo,tax,taxi,tc,tci,td,tdk,team,tech,technology,tel,temasek,tennis,teva,tf,tg,th,thd,theater,theatre,tiaa,tickets,tienda,tips,tires,tirol,tj,tjmaxx,tjx,tk,tkmaxx,tl,tm,tmall,tn,to,today,tokyo,tools,top,toray,toshiba,total,tours,town,toyota,toys,tr,trade,trading,training,travel,travelers,travelersinsurance,trust,trv,tt,tube,tui,tunes,tushu,tv,tvs,tw,tz,ua,ubank,ubs,ug,uk,unicom,university,uno,uol,ups,us,uy,uz,va,vacations,vana,vanguard,vc,ve,vegas,ventures,verisign,versicherung,vet,vg,vi,viajes,video,vig,viking,villas,vin,vip,virgin,visa,vision,viva,vivo,vlaanderen,vn,vodka,volvo,vote,voting,voto,voyage,vu,wales,walmart,walter,wang,wanggou,watch,watches,weather,weatherchannel,webcam,weber,website,wed,wedding,weibo,weir,wf,whoswho,wien,wiki,williamhill,win,windows,wine,winners,wme,wolterskluwer,woodside,work,works,world,wow,ws,wtc,wtf,xbox,xerox,xihuan,xin,xn--11b4c3d,xn--1ck2e1b,xn--1qqw23a,xn--2scrj9c,xn--30rr7y,xn--3bst00m,xn--3ds443g,xn--3e0b707e,xn--3hcrj9c,xn--3pxu8k,xn--42c2d9a,xn--45br5cyl,xn--45brj9c,xn--45q11c,xn--4dbrk0ce,xn--4gbrim,xn--54b7fta0cc,xn--55qw42g,xn--55qx5d,xn--5su34j936bgsg,xn--5tzm5g,xn--6frz82g,xn--6qq986b3xl,xn--80adxhks,xn--80ao21a,xn--80aqecdr1a,xn--80asehdb,xn--80aswg,xn--8y0a063a,xn--90a3ac,xn--90ae,xn--90ais,xn--9dbq2a,xn--9et52u,xn--9krt00a,xn--b4w605ferd,xn--bck1b9a5dre4c,xn--c1avg,xn--c2br7g,xn--cck2b3b,xn--cckwcxetd,xn--cg4bki,xn--clchc0ea0b2g2a9gcd,xn--czr694b,xn--czrs0t,xn--czru2d,xn--d1acj3b,xn--d1alf,xn--e1a4c,xn--eckvdtc9d,xn--efvy88h,xn--fct429k,xn--fhbei,xn--fiq228c5hs,xn--fiq64b,xn--fiqs8s,xn--fiqz9s,xn--fjq720a,xn--flw351e,xn--fpcrj9c3d,xn--fzc2c9e2c,xn--fzys8d69uvgm,xn--g2xx48c,xn--gckr3f0f,xn--gecrj9c,xn--gk3at1e,xn--h2breg3eve,xn--h2brj9c,xn--h2brj9c8c,xn--hxt814e,xn--i1b6b1a6a2e,xn--imr513n,xn--io0a7i,xn--j1aef,xn--j1amh,xn--j6w193g,xn--jlq480n2rg,xn--jvr189m,xn--kcrx77d1x4a,xn--kprw13d,xn--kpry57d,xn--kput3i,xn--l1acc,xn--lgbbat1ad8j,xn--mgb9awbf,xn--mgba3a3ejt,xn--mgba3a4f16a,xn--mgba7c0bbn0a,xn--mgbaam7a8h,xn--mgbab2bd,xn--mgbah1a3hjkrd,xn--mgbai9azgqp6j,xn--mgbayh7gpa,xn--mgbbh1a,xn--mgbbh1a71e,xn--mgbc0a9azcg,xn--mgbca7dzdo,xn--mgbcpq6gpa1a,xn--mgberp4a5d4ar,xn--mgbgu82a,xn--mgbi4ecexp,xn--mgbpl2fh,xn--mgbt3dhd,xn--mgbtx2b,xn--mgbx4cd0ab,xn--mix891f,xn--mk1bu44c,xn--mxtq1m,xn--ngbc5azd,xn--ngbe9e0a,xn--ngbrx,xn--node,xn--nqv7f,xn--nqv7fs00ema,xn--nyqy26a,xn--o3cw4h,xn--ogbpf8fl,xn--otu796d,xn--p1acf,xn--p1ai,xn--pgbs0dh,xn--pssy2u,xn--q7ce6a,xn--q9jyb4c,xn--qcka1pmc,xn--qxa6a,xn--qxam,xn--rhqv96g,xn--rovu88b,xn--rvc1e0am3e,xn--s9brj9c,xn--ses554g,xn--t60b56a,xn--tckwe,xn--tiq49xqyj,xn--unup4y,xn--vermgensberater-ctb,xn--vermgensberatung-pwb,xn--vhquv,xn--vuq861b,xn--w4r85el8fhu5dnra,xn--w4rs40l,xn--wgbh1c,xn--wgbl6a,xn--xhq521b,xn--xkc2al3hye2a,xn--xkc2dl3a5ee0h,xn--y9a3aq,xn--yfro4i67o,xn--ygbi2ammx,xn--zfr164b,xxx,xyz,yachts,yahoo,yamaxun,yandex,ye,yodobashi,yoga,yokohama,you,youtube,yt,yun,za,zappos,zara,zero,zip,zm,zone,zuerich,zw"
    // 切分
    .split(',')

/**
 *
 * 该代码从 https://zh-hans.tld-list.com/%E5%9F%9F%E5%90%8D%E4%BB%8EA%E5%88%B0Z%E6%8E%92%E5%88%97 上下载
 * 在控制台中执行代码可以获得：
 *
 * copy([...$$('ul.feature-list')].map(it=>it.textContent.trim().replace(/[\n\s]+/g,',')).join(',').split(',').map((it)=>it.slice(1)).filter((it)=>it.includes('.')).join(','))
 *
 * 该网址也可以用于获得所有的顶级域名：
 * copy([...$$('ul.feature-list')].map(it=>it.textContent.trim().replace(/[\n\s]+/g,',')).join(',').split(',').map((it)=>it.slice(1)).join(','))
 *
 * 注意，这里没有转译
 */
private val twoTopLevel =
  "ac.ae,co.ae,net.ae,org.ae,sch.ae,cargo.aero,charter.aero,com.af,edu.af,gov.af,net.af,org.af,co.ag,com.ag,net.ag,nom.ag,org.ag,com.ai,net.ai,off.ai,org.ai,com.al,edu.al,net.al,org.al,co.am,com.am,net.am,north.am,org.am,radio.am,south.am,co.ao,it.ao,og.ao,pb.ao,com.ar,int.ar,net.ar,org.ar,co.at,or.at,asn.au,com.au,id.au,info.au,net.au,org.au,com.aw,biz.az,co.az,com.az,edu.az,gov.az,info.az,int.az,mil.az,name.az,net.az,org.az,pp.az,pro.az,co.ba,com.ba,co.bb,com.bb,net.bb,org.bb,ac.bd,com.bd,net.bd,org.bd,biz.bh,cc.bh,com.bh,edu.bh,me.bh,name.bh,net.bh,org.bh,co.bi,com.bi,edu.bi,info.bi,mo.bi,net.bi,or.bi,org.bi,auz.biz,com.bj,edu.bj,com.bm,net.bm,org.bm,com.bn,net.bn,org.bn,com.bo,net.bo,org.bo,tv.bo,abc.br,adm.br,adv.br,agr.br,am.br,aparecida.br,app.br,arq.br,art.br,ato.br,belem.br,bhz.br,bib.br,bio.br,blog.br,bmd.br,boavista.br,bsb.br,campinas.br,caxias.br,cim.br,cng.br,cnt.br,com.br,coop.br,curitiba.br,des.br,det.br,dev.br,ecn.br,eco.br,edu.br,emp.br,enf.br,eng.br,esp.br,etc.br,eti.br,far.br,flog.br,floripa.br,fm.br,fnd.br,fortal.br,fot.br,foz.br,fst.br,g12.br,geo.br,ggf.br,gov.br,gru.br,imb.br,ind.br,inf.br,jampa.br,jor.br,lel.br,log.br,macapa.br,maceio.br,manaus.br,mat.br,med.br,mil.br,mus.br,natal.br,net.br,nom.br,not.br,ntr.br,odo.br,org.br,palmas.br,poa.br,ppg.br,pro.br,psc.br,psi.br,qsl.br,radio.br,rec.br,recife.br,rep.br,rio.br,salvador.br,seg.br,sjc.br,slg.br,srv.br,taxi.br,tec.br,teo.br,tmp.br,trd.br,tur.br,tv.br,vet.br,vix.br,vlog.br,wiki.br,zlg.br,com.bs,net.bs,org.bs,com.bt,org.bt,ac.bw,co.bw,net.bw,org.bw,com.by,minsk.by,net.by,co.bz,com.bz,net.bz,org.bz,za.bz,com.cd,net.cd,org.cd,ac.ci,co.ci,com.ci,ed.ci,edu.ci,go.ci,in.ci,int.ci,net.ci,nom.ci,or.ci,org.ci,biz.ck,co.ck,edu.ck,gen.ck,gov.ck,info.ck,net.ck,org.ck,co.cm,com.cm,net.cm,ac.cn,ah.cn,bj.cn,com.cn,cq.cn,fj.cn,gd.cn,gs.cn,gx.cn,gz.cn,ha.cn,hb.cn,he.cn,hi.cn,hk.cn,hl.cn,hn.cn,jl.cn,js.cn,jx.cn,ln.cn,mo.cn,net.cn,nm.cn,nx.cn,org.cn,qh.cn,sc.cn,sd.cn,sh.cn,sn.cn,sx.cn,tj.cn,tw.cn,xj.cn,xz.cn,yn.cn,zj.cn,com.co,net.co,nom.co,ae.com,africa.com,br.com,cn.com,co.com,de.com,eu.com,gr.com,hk.com,hu.com,it.com,jpn.com,kr.com,mex.com,no.com,nv.com,pty-ltd.com,qc.com,ru.com,sa.com,se.com,uk.com,us.com,za.com,co.cr,ed.cr,fi.cr,go.cr,or.cr,sa.cr,com.cu,com.cv,edu.cv,int.cv,net.cv,nome.cv,org.cv,publ.cv,com.cw,net.cw,ac.cy,biz.cy,com.cy,ekloges.cy,ltd.cy,name.cy,net.cy,org.cy,parliament.cy,press.cy,pro.cy,tm.cy,co.cz,co.de,com.de,biz.dk,co.dk,co.dm,com.dm,net.dm,org.dm,art.do,com.do,net.do,org.do,sld.do,web.do,com.dz,com.ec,fin.ec,info.ec,med.ec,net.ec,org.ec,pro.ec,co.ee,com.ee,fie.ee,med.ee,pri.ee,com.eg,edu.eg,eun.eg,gov.eg,info.eg,name.eg,net.eg,org.eg,tv.eg,com.es,edu.es,gob.es,nom.es,org.es,biz.et,com.et,info.et,name.et,net.et,org.et,ac.fj,biz.fj,com.fj,info.fj,name.fj,net.fj,org.fj,pro.fj,co.fk,radio.fm,aeroport.fr,asso.fr,avocat.fr,chambagri.fr,chirurgiens-dentistes.fr,com.fr,experts-comptables.fr,geometre-expert.fr,gouv.fr,medecin.fr,nom.fr,notaires.fr,pharmacien.fr,port.fr,prd.fr,presse.fr,tm.fr,veterinaire.fr,com.ge,edu.ge,gov.ge,mil.ge,net.ge,org.ge,pvt.ge,co.gg,net.gg,org.gg,com.gh,edu.gh,gov.gh,org.gh,com.gi,gov.gi,ltd.gi,org.gi,co.gl,com.gl,edu.gl,net.gl,org.gl,com.gn,gov.gn,net.gn,org.gn,com.gp,mobi.gp,net.gp,org.gp,com.gr,edu.gr,net.gr,org.gr,com.gt,ind.gt,net.gt,org.gt,com.gu,co.gy,com.gy,net.gy,com.hk,edu.hk,gov.hk,idv.hk,inc.hk,ltd.hk,net.hk,org.hk,公司.hk,com.hn,edu.hn,net.hn,org.hn,com.hr,adult.ht,art.ht,asso.ht,com.ht,edu.ht,firm.ht,info.ht,net.ht,org.ht,perso.ht,pol.ht,pro.ht,rel.ht,shop.ht,2000.hu,agrar.hu,bolt.hu,casino.hu,city.hu,co.hu,erotica.hu,erotika.hu,film.hu,forum.hu,games.hu,hotel.hu,info.hu,ingatlan.hu,jogasz.hu,konyvelo.hu,lakas.hu,media.hu,news.hu,org.hu,priv.hu,reklam.hu,sex.hu,shop.hu,sport.hu,suli.hu,szex.hu,tm.hu,tozsde.hu,utazas.hu,video.hu,biz.id,co.id,my.id,or.id,web.id,ac.il,co.il,muni.il,net.il,org.il,ac.im,co.im,com.im,ltd.co.im,net.im,org.im,plc.co.im,5g.in,6g.in,ahmdabad.in,ai.in,am.in,bihar.in,biz.in,business.in,ca.in,cn.in,co.in,com.in,coop.in,cs.in,delhi.in,dr.in,er.in,firm.in,gen.in,gujarat.in,ind.in,info.in,int.in,internet.in,io.in,me.in,net.in,org.in,pg.in,post.in,pro.in,travel.in,tv.in,uk.in,up.in,us.in,auz.info,com.iq,co.ir,abr.it,abruzzo.it,ag.it,agrigento.it,al.it,alessandria.it,alto-adige.it,altoadige.it,an.it,ancona.it,andria-barletta-trani.it,andria-trani-barletta.it,andriabarlettatrani.it,andriatranibarletta.it,ao.it,aosta.it,aoste.it,ap.it,aq.it,aquila.it,ar.it,arezzo.it,ascoli-piceno.it,ascolipiceno.it,asti.it,at.it,av.it,avellino.it,ba.it,balsan.it,bari.it,barletta-trani-andria.it,barlettatraniandria.it,bas.it,basilicata.it,belluno.it,benevento.it,bergamo.it,bg.it,bi.it,biella.it,bl.it,bn.it,bo.it,bologna.it,bolzano.it,bozen.it,br.it,brescia.it,brindisi.it,bs.it,bt.it,bz.it,ca.it,cagliari.it,cal.it,calabria.it,caltanissetta.it,cam.it,campania.it,campidano-medio.it,campidanomedio.it,campobasso.it,carbonia-iglesias.it,carboniaiglesias.it,carrara-massa.it,carraramassa.it,caserta.it,catania.it,catanzaro.it,cb.it,ce.it,cesena-forli.it,cesenaforli.it,ch.it,chieti.it,ci.it,cl.it,cn.it,co.it,como.it,cosenza.it,cr.it,cremona.it,crotone.it,cs.it,ct.it,cuneo.it,cz.it,dell-ogliastra.it,dellogliastra.it,emilia-romagna.it,emiliaromagna.it,emr.it,en.it,enna.it,fc.it,fe.it,fermo.it,ferrara.it,fg.it,fi.it,firenze.it,florence.it,fm.it,foggia.it,forli-cesena.it,forlicesena.it,fr.it,friuli-v-giulia.it,friuli-ve-giulia.it,friuli-vegiulia.it,friuli-venezia-giulia.it,friuli-veneziagiulia.it,friuli-vgiulia.it,friuliv-giulia.it,friulive-giulia.it,friulivegiulia.it,friulivenezia-giulia.it,friuliveneziagiulia.it,friulivgiulia.it,frosinone.it,fvg.it,ge.it,genoa.it,genova.it,go.it,gorizia.it,gr.it,grosseto.it,iglesias-carbonia.it,iglesiascarbonia.it,im.it,imperia.it,is.it,isernia.it,kr.it,la-spezia.it,laquila.it,laspezia.it,latina.it,laz.it,lazio.it,lc.it,le.it,lecce.it,lecco.it,li.it,lig.it,liguria.it,livorno.it,lo.it,lodi.it,lom.it,lombardia.it,lombardy.it,lt.it,lu.it,lucania.it,lucca.it,macerata.it,mantova.it,mar.it,marche.it,massa-carrara.it,massacarrara.it,matera.it,mb.it,mc.it,me.it,medio-campidano.it,mediocampidano.it,messina.it,mi.it,milan.it,milano.it,mn.it,mo.it,modena.it,mol.it,molise.it,monza-brianza.it,monza-e-della-brianza.it,monza.it,monzabrianza.it,monzaebrianza.it,monzaedellabrianza.it,ms.it,mt.it,na.it,naples.it,napoli.it,no.it,novara.it,nu.it,nuoro.it,og.it,ogliastra.it,olbia-tempio.it,olbiatempio.it,or.it,oristano.it,ot.it,pa.it,padova.it,padua.it,palermo.it,parma.it,pavia.it,pc.it,pd.it,pe.it,perugia.it,pesaro-urbino.it,pesarourbino.it,pescara.it,pg.it,pi.it,piacenza.it,piedmont.it,piemonte.it,pisa.it,pistoia.it,pmn.it,pn.it,po.it,pordenone.it,potenza.it,pr.it,prato.it,pt.it,pu.it,pug.it,puglia.it,pv.it,pz.it,ra.it,ragusa.it,ravenna.it,rc.it,re.it,reggio-calabria.it,reggio-emilia.it,reggiocalabria.it,reggioemilia.it,rg.it,ri.it,rieti.it,rimini.it,rm.it,rn.it,ro.it,roma.it,rome.it,rovigo.it,sa.it,salerno.it,sar.it,sardegna.it,sardinia.it,sassari.it,savona.it,si.it,sic.it,sicilia.it,sicily.it,siena.it,siracusa.it,so.it,sondrio.it,sp.it,sr.it,ss.it,suedtirol.it,sv.it,ta.it,taa.it,taranto.it,te.it,tempio-olbia.it,tempioolbia.it,teramo.it,terni.it,tn.it,to.it,torino.it,tos.it,toscana.it,tp.it,tr.it,trani-andria-barletta.it,trani-barletta-andria.it,traniandriabarletta.it,tranibarlettaandria.it,trapani.it,trentino-a-adige.it,trentino-aadige.it,trentino-alto-adige.it,trentino-altoadige.it,trentino-s-tirol.it,trentino-stirol.it,trentino-sud-tirol.it,trentino-sudtirol.it,trentino-sued-tirol.it,trentino-suedtirol.it,trentino.it,trentinoa-adige.it,trentinoaadige.it,trentinoalto-adige.it,trentinoaltoadige.it,trentinos-tirol.it,trentinosud-tirol.it,trentinosudtirol.it,trentinosued-tirol.it,trentinosuedtirol.it,trento.it,treviso.it,trieste.it,ts.it,turin.it,tuscany.it,tv.it,ud.it,udine.it,umb.it,umbria.it,urbino-pesaro.it,urbinopesaro.it,va.it,val-d-aosta.it,val-daosta.it,vald-aosta.it,valdaosta.it,valle-d-aosta.it,valle-daosta.it,valled-aosta.it,valledaosta.it,vao.it,varese.it,vb.it,vc.it,vda.it,ve.it,ven.it,veneto.it,venezia.it,venice.it,verbania.it,vercelli.it,verona.it,vi.it,vibo-valentia.it,vibovalentia.it,vicenza.it,viterbo.it,vr.it,vs.it,vt.it,vv.it,co.je,net.je,org.je,com.jm,net.jm,org.jm,com.jo,name.jo,net.jo,org.jo,sch.jo,akita.jp,co.jp,gr.jp,kyoto.jp,ne.jp,or.jp,osaka.jp,saga.jp,tokyo.jp,ac.ke,co.ke,go.ke,info.ke,me.ke,mobi.ke,ne.ke,or.ke,sc.ke,com.kg,net.kg,org.kg,com.kh,edu.kh,net.kh,org.kh,biz.ki,com.ki,edu.ki,gov.ki,info.ki,mobi.ki,net.ki,org.ki,phone.ki,tel.ki,com.km,nom.km,org.km,tm.km,com.kn,edu.kn,gov.kn,co.kr,go.kr,ms.kr,ne.kr,or.kr,pe.kr,re.kr,seoul.kr,com.kw,edu.kw,net.kw,org.kw,com.ky,net.ky,org.ky,com.kz,org.kz,com.lb,edu.lb,net.lb,org.lb,co.lc,com.lc,l.lc,net.lc,org.lc,p.lc,assn.lk,com.lk,edu.lk,grp.lk,hotel.lk,ltd.lk,ngo.lk,org.lk,soc.lk,web.lk,com.lr,org.lr,co.ls,net.ls,org.ls,asn.lv,com.lv,conf.lv,edu.lv,id.lv,mil.lv,net.lv,org.lv,com.ly,id.ly,med.ly,net.ly,org.ly,plc.ly,sch.ly,ac.ma,co.ma,net.ma,org.ma,press.ma,asso.mc,tm.mc,co.mg,com.mg,mil.mg,net.mg,nom.mg,org.mg,prd.mg,tm.mg,com.mk,edu.mk,inf.mk,net.mk,org.mk,com.ml,info.ml,net.ml,org.ml,biz.mm,com.mm,net.mm,org.mm,per.mm,com.mo,net.mo,org.mo,edu.mr,org.mr,perso.mr,co.ms,com.ms,org.ms,com.mt,edu.mt,net.mt,org.mt,ac.mu,co.mu,com.mu,net.mu,nom.mu,or.mu,org.mu,com.mv,ac.mw,co.mw,com.mw,coop.mw,edu.mw,int.mw,net.mw,org.mw,com.mx,edu.mx,gob.mx,net.mx,org.mx,com.my,mil.my,name.my,net.my,org.my,co.mz,edu.mz,net.mz,org.mz,alt.na,cc.na,co.na,com.na,edu.na,info.na,net.na,org.na,school.na,com.ne,info.ne,int.ne,org.ne,perso.ne,auz.net,gb.net,hu.net,in.net,jp.net,ru.net,se.net,uk.net,arts.nf,com.nf,firm.nf,info.nf,net.nf,org.nf,other.nf,per.nf,rec.nf,store.nf,web.nf,com.ng,edu.ng,gov.ng,i.ng,lg.gov.ng,mobi.ng,name.ng,net.ng,org.ng,sch.ng,ac.ni,biz.ni,co.ni,com.ni,edu.ni,gob.ni,in.ni,info.ni,int.ni,mil.ni,net.ni,nom.ni,org.ni,web.ni,co.nl,com.nl,net.nl,co.no,fhs.no,folkebibl.no,fylkesbibl.no,gs.no,idrett.no,museum.no,priv.no,uenorge.no,vgs.no,aero.np,asia.np,biz.np,com.np,coop.np,info.np,mil.np,mobi.np,museum.np,name.np,net.np,org.np,pro.np,travel.np,biz.nr,com.nr,info.nr,net.nr,org.nr,co.nu,ac.nz,co.net.nz,co.nz,geek.nz,gen.nz,iwi.nz,kiwi.nz,maori.nz,net.nz,org.nz,school.nz,biz.om,co.om,com.om,edu.om,gov.om,med.om,mil.om,museum.om,net.om,org.om,pro.om,sch.om,ae.org,hk.org,us.org,abo.pa,com.pa,edu.pa,gob.pa,ing.pa,med.pa,net.pa,nom.pa,org.pa,sld.pa,com.pe,edu.pe,gob.pe,mil.pe,net.pe,nom.pe,org.pe,asso.pf,com.pf,edu.pf,gov.pf,org.pf,com.pg,net.pg,org.pg,com.ph,net.ph,org.ph,biz.pk,com.pk,net.pk,org.pk,web.pk,agro.pl,aid.pl,atm.pl,augustow.pl,auto.pl,babia-gora.pl,bedzin.pl,beskidy.pl,bialowieza.pl,bialystok.pl,bielawa.pl,bieszczady.pl,biz.pl,boleslawiec.pl,bydgoszcz.pl,bytom.pl,cieszyn.pl,com.pl,czeladz.pl,czest.pl,dlugoleka.pl,edu.pl,elblag.pl,elk.pl,glogow.pl,gmina.pl,gniezno.pl,gorlice.pl,grajewo.pl,gsm.pl,ilawa.pl,info.pl,jaworzno.pl,jelenia-gora.pl,jgora.pl,kalisz.pl,karpacz.pl,kartuzy.pl,kaszuby.pl,katowice.pl,kazimierz-dolny.pl,kepno.pl,ketrzyn.pl,klodzko.pl,kobierzyce.pl,kolobrzeg.pl,konin.pl,konskowola.pl,kutno.pl,lapy.pl,lebork.pl,legnica.pl,lezajsk.pl,limanowa.pl,lomza.pl,lowicz.pl,lubin.pl,lukow.pl,mail.pl,malbork.pl,malopolska.pl,mazowsze.pl,mazury.pl,media.pl,miasta.pl,mielec.pl,mielno.pl,mil.pl,mragowo.pl,naklo.pl,net.pl,nieruchomosci.pl,nom.pl,nowaruda.pl,nysa.pl,olawa.pl,olecko.pl,olkusz.pl,olsztyn.pl,opoczno.pl,opole.pl,org.pl,ostroda.pl,ostroleka.pl,ostrowiec.pl,ostrowwlkp.pl,pc.pl,pila.pl,pisz.pl,podhale.pl,podlasie.pl,polkowice.pl,pomorskie.pl,pomorze.pl,powiat.pl,priv.pl,prochowice.pl,pruszkow.pl,przeworsk.pl,pulawy.pl,radom.pl,rawa-maz.pl,realestate.pl,rel.pl,rybnik.pl,rzeszow.pl,sanok.pl,sejny.pl,sex.pl,shop.pl,sklep.pl,skoczow.pl,slask.pl,slupsk.pl,sos.pl,sosnowiec.pl,stalowa-wola.pl,starachowice.pl,stargard.pl,suwalki.pl,swidnica.pl,swiebodzin.pl,swinoujscie.pl,szczecin.pl,szczytno.pl,szkola.pl,targi.pl,tarnobrzeg.pl,tgory.pl,tm.pl,tourism.pl,travel.pl,turek.pl,turystyka.pl,tychy.pl,ustka.pl,walbrzych.pl,warmia.pl,warszawa.pl,waw.pl,wegrow.pl,wielun.pl,wlocl.pl,wloclawek.pl,wodzislaw.pl,wolomin.pl,wroclaw.pl,zachpomor.pl,zagan.pl,zarow.pl,zgora.pl,zgorzelec.pl,co.pn,net.pn,org.pn,com.post,edu.post,org.post,at.pr,biz.pr,ch.pr,com.pr,de.pr,eu.pr,fr.pr,info.pr,isla.pr,it.pr,name.pr,net.pr,nl.pr,org.pr,pro.pr,uk.pr,aaa.pro,aca.pro,acct.pro,arc.pro,avocat.pro,bar.pro,bus.pro,chi.pro,chiro.pro,cpa.pro,den.pro,dent.pro,eng.pro,jur.pro,law.pro,med.pro,min.pro,nur.pro,nurse.pro,pharma.pro,prof.pro,prx.pro,recht.pro,rel.pro,teach.pro,vet.pro,com.ps,net.ps,org.ps,co.pt,com.pt,org.pt,com.py,coop.py,edu.py,net.py,org.py,com.qa,edu.qa,mil.qa,name.qa,net.qa,org.qa,sch.qa,arts.ro,co.ro,com.ro,firm.ro,info.ro,ne.ro,nom.ro,nt.ro,or.ro,org.ro,rec.ro,sa.ro,srl.ro,store.ro,tm.ro,www.ro,co.rs,edu.rs,in.rs,org.rs,adygeya.ru,bashkiria.ru,bir.ru,cbg.ru,com.ru,dagestan.ru,grozny.ru,kalmykia.ru,kustanai.ru,marine.ru,mordovia.ru,msk.ru,mytis.ru,nalchik.ru,net.ru,nov.ru,org.ru,pp.ru,pyatigorsk.ru,spb.ru,vladikavkaz.ru,vladimir.ru,ac.rw,co.rw,net.rw,org.rw,com.sa,edu.sa,med.sa,net.sa,org.sa,pub.sa,sch.sa,com.sb,net.sb,org.sb,com.sc,net.sc,org.sc,com.sd,info.sd,net.sd,com.se,com.sg,edu.sg,net.sg,org.sg,ae.si,at.si,cn.si,co.si,de.si,uk.si,us.si,org.sk,com.sl,edu.sl,net.sl,org.sl,art.sn,com.sn,edu.sn,org.sn,perso.sn,univ.sn,com.so,net.so,org.so,biz.ss,com.ss,me.ss,net.ss,abkhazia.su,adygeya.su,aktyubinsk.su,arkhangelsk.su,armenia.su,ashgabad.su,azerbaijan.su,balashov.su,bashkiria.su,bryansk.su,bukhara.su,chimkent.su,dagestan.su,east-kazakhstan.su,exnet.su,georgia.su,grozny.su,ivanovo.su,jambyl.su,kalmykia.su,kaluga.su,karacol.su,karaganda.su,karelia.su,khakassia.su,krasnodar.su,kurgan.su,kustanai.su,lenug.su,mangyshlak.su,mordovia.su,msk.su,murmansk.su,nalchik.su,navoi.su,north-kazakhstan.su,nov.su,obninsk.su,penza.su,pokrovsk.su,sochi.su,spb.su,tashkent.su,termez.su,togliatti.su,troitsk.su,tselinograd.su,tula.su,tuva.su,vladikavkaz.su,vladimir.su,vologda.su,com.sv,edu.sv,gob.sv,org.sv,com.sy,co.sz,org.sz,com.tc,net.tc,org.tc,pro.tc,com.td,net.td,org.td,tourism.td,ac.th,co.th,in.th,or.th,ac.tj,aero.tj,biz.tj,co.tj,com.tj,coop.tj,dyn.tj,go.tj,info.tj,int.tj,mil.tj,museum.tj,my.tj,name.tj,net.tj,org.tj,per.tj,pro.tj,web.tj,com.tl,net.tl,org.tl,agrinet.tn,com.tn,defense.tn,edunet.tn,ens.tn,fin.tn,ind.tn,info.tn,intl.tn,nat.tn,net.tn,org.tn,perso.tn,rnrt.tn,rns.tn,rnu.tn,tourism.tn,av.tr,bbs.tr,biz.tr,com.tr,dr.tr,gen.tr,info.tr,name.tr,net.tr,org.tr,tel.tr,tv.tr,web.tr,biz.tt,co.tt,com.tt,info.tt,jobs.tt,mobi.tt,name.tt,net.tt,org.tt,pro.tt,club.tw,com.tw,ebiz.tw,game.tw,idv.tw,mil.tw,net.tw,org.tw,ac.tz,co.tz,go.tz,hotel.tz,info.tz,me.tz,mil.tz,mobi.tz,ne.tz,or.tz,sc.tz,tv.tz,biz.ua,cherkassy.ua,cherkasy.ua,chernigov.ua,chernivtsi.ua,chernovtsy.ua,ck.ua,cn.ua,co.ua,com.ua,crimea.ua,cv.ua,dn.ua,dnepropetrovsk.ua,dnipropetrovsk.ua,donetsk.ua,dp.ua,edu.ua,gov.ua,if.ua,in.ua,ivano-frankivsk.ua,kh.ua,kharkiv.ua,kharkov.ua,kherson.ua,khmelnitskiy.ua,kiev.ua,kirovograd.ua,km.ua,kr.ua,ks.ua,kyiv.ua,lg.ua,lt.ua,lugansk.ua,lutsk.ua,lviv.ua,mk.ua,net.ua,nikolaev.ua,od.ua,odesa.ua,odessa.ua,org.ua,pl.ua,poltava.ua,pp.ua,rivne.ua,rovno.ua,rv.ua,sebastopol.ua,sm.ua,sumy.ua,te.ua,ternopil.ua,uz.ua,uzhgorod.ua,vinnica.ua,vn.ua,volyn.ua,yalta.ua,zaporizhzhe.ua,zhitomir.ua,zp.ua,zt.ua,ac.ug,co.ug,com.ug,go.ug,ne.ug,or.ug,org.ug,sc.ug,ac.uk,barking-dagenham.sch.uk,barnet.sch.uk,barnsley.sch.uk,bathnes.sch.uk,beds.sch.uk,bexley.sch.uk,bham.sch.uk,blackburn.sch.uk,blackpool.sch.uk,bolton.sch.uk,bournemouth.sch.uk,bracknell-forest.sch.uk,bradford.sch.uk,brent.sch.uk,co.uk,doncaster.sch.uk,gov.uk,ltd.uk,me.uk,net.uk,org.uk,plc.uk,sch.uk,com.uy,edu.uy,net.uy,org.uy,biz.uz,co.uz,com.uz,net.uz,org.uz,com.vc,net.vc,org.vc,co.ve,com.ve,info.ve,net.ve,org.ve,web.ve,co.vi,com.vi,net.vi,org.vi,ac.vn,biz.vn,com.vn,edu.vn,gov.vn,health.vn,info.vn,int.vn,name.vn,net.vn,org.vn,pro.vn,com.vu,net.vu,org.vu,com.ws,net.ws,org.ws,com.ye,net.ye,org.ye,co.za,net.za,org.za,web.za,co.zm,com.zm,org.zm,co.zw,org.zw,ком.рф,нет.рф,орг.рф,ак.срб,пр.срб,упр.срб,كمپنی.بھارت,कंपनी.भारत,কোম্পানি.ভারত,ਕੰਪਨੀ.ਭਾਰਤ,કંપની.ભારત,நிறுவனம்.இந்தியா,కంపెనీ.భారత్,ธุรกิจ.ไทย,個人.香港,公司.香港,政府.香港,教育.香港,組織.香港,網絡.香港"
    // 切分
    .split(',').map { it.toPunyCode() }

public class DomainParser(domainName: String) {
  private var input = domainName

  /**
   * 二级域名
   */
  public var secondLevelDomain: String = ""
    private set
  public var hasError: Boolean = false
    private set
  public var errorMessage: String = ""
    private set

  internal fun parse() {
    if (input.isEmpty()) {
      failMessage("请填写域名，例如：sojson.com")
      return
    }
    val labels = parseLabels() ?: return
    if (hasError) {
      return
    }
    if (labels.size == 1) {
      failMessage("域名格式错误。请输入正确的域名格式，以“.”进行区分 ")
      return
    }
    if (isDigitLabels(labels.last())) {
      failMessage("顶级域名格式错误。请输入正确的域名格式，以“.”进行区分 ")
      return
    }
    if (input.length > 255) {
      failMessage("域名过长。每标号不得超过63个字符。由多个标号组成的完整域名总共不超过255个字符。")
      return
    }
    val topLevel = parseTopLevel(labels) ?: return
    if (topLevel.labelIndex == 0) {
      failMessage("${topLevel.name}是域名后缀，不能查询。")
      return
    }
    val secondLevel = parseSecondLevel(labels, topLevel)
    if (secondLevel.labelIndex != 0 && topLevel.recognized) {
      secondLevelDomain = "${secondLevel.name}.${topLevel.name}"
    }
  }

  private fun parseLabels(): List<String>? {
    val labels = mutableListOf<String>()
    var offset = 0
    while (offset < input.length) {
      val (label, endOffset) = parseLabel(offset) ?: return null
      labels.add(label)
      offset = endOffset
      // Skip '.' to start parsing the next label.
      if (offset < input.length && input[offset] == '.') {
        offset++
      }
    }
    return labels
  }

  private fun parseLabel(startOffset: Int): Pair<String, Int>? {
    val labelArr = mutableListOf<Char>()
    var offset = startOffset
    while (offset < input.length && input[offset] != '.') {
      val ch = input[offset]
      val invalid = when {
        startOffset == offset && !isLetterOrDigit(ch) -> true
        // offset + 1 == input.length || input[offset + 1] == '.' && !isLetterOrDigit(ch) -> true // TODO 这个判断没明白意思
        offset + 1 == input.length && !isLetterOrDigit(ch) -> true // 最后一位，必须是字母、数字，不能是其他符号，不然也是无效的
        !isLabelChar(ch) -> true
        else -> false
      }
      if (invalid) {
        failMessage(
          "格式错误。域名一般由英文字母、汉字、阿拉伯数字、" + "-组成，用“.”分隔，且每段不能以“.”、“-”开头和结尾"
        )
        return null
      } else {
        labelArr.add(ch)
        offset++
      }
    }
    if (labelArr.size > 63) {
      failMessage("域名过长。每标号不得超过63个字符。由多个标号组成的完整域名总共不超过255个字符。")
      return null
    }
    return labelArr.joinToString("") to offset
  }

  private fun isLabelChar(ch: Char): Boolean {
    val charCode = ch.code
    return if (charCode <= 127) {
      ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-'
    } else {
      !(charCode in 0xFF00..0xFFEF || charCode in 0x3000..0x303F)
    }
  }

  private fun isLetterOrDigit(ch: Char): Boolean {
    val charCode = ch.code
    return if (charCode <= 127) {
      ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9'
    } else {
      !(charCode in 0xFF00..0xFFEF || charCode in 0x3000..0x303F)
    }
  }

  private fun isDigitLabels(label: String): Boolean {
    return label.all { it.isDigit() }
  }

  private fun parseTopLevel(labels: List<String>): TopLevel? {
    var topLevelName: String
    var lowTopLevelName: String
    var topLevel: TopLevel? = null
    if (labels.size >= 2) {
      topLevelName = labels.takeLast(2).joinToString(".")
      lowTopLevelName = topLevelName.lowercase();
      if (twoTopLevel.contains(lowTopLevelName)) {
        topLevel = TopLevel(topLevelName, 2, labels.size - 2, true);
      }
    }
    if (null != topLevel) {
      topLevelName = labels.last();
      lowTopLevelName = topLevelName.lowercase();
      if (oneTopLevel.contains(lowTopLevelName)) {
        topLevel = TopLevel(topLevelName, 1, labels.size - 1, true);
      }
    }
    if (null != topLevel) {
      topLevelName = labels.last()
      topLevel = TopLevel(topLevelName, 1, labels.size - 1, false);
    }
    return topLevel;
  }

  private fun parseSecondLevel(labels: List<String>, topLevel: TopLevel): SecondLevel {
    return SecondLevel(labels[topLevel.labelIndex - 1], 1, topLevel.labelIndex - 1)
  }

  private fun failMessage(msg: String) {
    errorMessage = msg
    hasError = true
  }

  /**
   * 顶级域名
   */
  private data class TopLevel(
    val name: String,
    val labelCount: Int,
    val labelIndex: Int,
    val recognized: Boolean,
  )

  /**
   * 二级域名
   */
  private data class SecondLevel(
    val name: String,
    val labelCount: Int,
    val labelIndex: Int,
  )
}
